package com.karton.delivery.database;

import com.karton.delivery.KartonDeliveryAPI;
import com.karton.delivery.ServerContextHolder;
import com.karton.delivery.threading.DatabaseExecutor;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class DeliveriesDatabase {
    // Читать и изменять этот список только в серверном потоке.
    private static final ArrayList<DeliveryEntry> cachedDeliveries =
            new ArrayList<>();

    // Вызвать до запуска остальных запросов.
    public static void initTables() throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS deliveries (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    item TEXT,
                    sender TEXT,
                    target TEXT
                )
                """;

        try (Statement stmt = ConnectionManager.connection.createStatement()) {
            stmt.execute(sql);
        }
    }

    private static int send(
            String itemJson,
            String sender,
            String target
    ) {
        String sql = """
                INSERT INTO deliveries (item, sender, target)
                VALUES (?, ?, ?)
                """;

        try (PreparedStatement pstmt =
                     ConnectionManager.connection.prepareStatement(
                             sql,
                             Statement.RETURN_GENERATED_KEYS
                     )) {
            pstmt.setString(1, itemJson);
            pstmt.setString(2, sender);
            pstmt.setString(3, target);

            if (pstmt.executeUpdate() != 1) {
                throw new SQLException("Can't add delivery entry");
            }

            // Закрываем ResultSet явно.
            try (ResultSet keys = pstmt.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }

            throw new SQLException("Delivery entry did not return a new id");
        } catch (SQLException e) {
            // Ошибка завершает future exceptionally вместо возврата -1.
            throw new CompletionException("Cannot insert delivery", e);
        }
    }

    public static CompletableFuture<Integer> sendAsync(
            DeliveryEntry delivery
    ) {
        try {
            String itemJson = delivery.serializeItem();
            String sender = delivery.sender();
            String target = delivery.target();

            return CompletableFuture.supplyAsync(
                    () -> send(itemJson, sender, target),
                    DatabaseExecutor.executor()
            );
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    public static CompletableFuture<Integer> sendAndCahceAsync(
            DeliveryEntry delivery
    ) {
        try {
            String itemJson = delivery.serializeItem();
            String sender = delivery.sender();
            String target = delivery.target();

            return executeAndUpdateCache(
                    () -> send(itemJson, sender, target),
                    id -> cachedDeliveries.add(delivery.withId(id)),
                    "Cannot insert and cache delivery"
            );
        } catch (Exception e) {
            KartonDeliveryAPI.LOGGER.error(
                    "Cannot prepare delivery for insertion", e
            );
            return CompletableFuture.failedFuture(e);
        }
    }

    private static ArrayList<DeliveryEntry> getDeliveries() {
        String sql = """
                SELECT id, item, sender, target FROM deliveries
                """;

        ArrayList<DeliveryEntry> entries = new ArrayList<>();

        try (
                Statement stmt = ConnectionManager.connection.createStatement();
                ResultSet rs = stmt.executeQuery(sql)
        ) {
            while (rs.next()) {
                entries.add(
                        DeliveryEntry.fromRaw(
                                rs.getString("item"),
                                rs.getString("sender"),
                                rs.getString("target"),
                                rs.getInt("id")
                        )
                );
            }

            return entries;
        } catch (SQLException | CommandSyntaxException e) {
            throw new CompletionException("Cannot load deliveries", e);
        }
    }

    public static CompletableFuture<ArrayList<DeliveryEntry>> getDeliveriesAsync() {
        return CompletableFuture.supplyAsync(
                DeliveriesDatabase::getDeliveries,
                DatabaseExecutor.executor()
        );
    }

    public static CompletableFuture<Void> loadDeliveriesToCache() {
        return executeAndUpdateCache(
                DeliveriesDatabase::getDeliveries,
                deliveries -> {
                    cachedDeliveries.clear();
                    cachedDeliveries.addAll(deliveries);
                },
                "Cannot load deliveries into cache"
        ).thenAccept(ignored -> {});
    }

    public static ArrayList<DeliveryEntry> getCachedDeliveries() {
        return cachedDeliveries;
    }

    private static void deleteDelivery(int id) {
        String sql = """
                DELETE FROM deliveries WHERE id = ?
                """;

        try (PreparedStatement pstmt =
                     ConnectionManager.connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);

            if (pstmt.executeUpdate() != 1) {
                throw new SQLException(
                        "Delivery does not exist or was already deleted: id=" + id
                );
            }
        } catch (SQLException e) {
            throw new CompletionException(
                    "Cannot delete delivery with id=" + id, e
            );
        }
    }

    // Только удаление из БД, без изменения кэша.
    public static CompletableFuture<Void> deleteDeliveryAsync(int id) {
        return CompletableFuture.runAsync(
                () -> deleteDelivery(id),
                DatabaseExecutor.executor()
        );
    }

    // Название сохранено для совместимости. Метод асинхронный!
    public static CompletableFuture<Void> deleteDeliveryAndCacheSync(int id) {
        return executeAndUpdateCache(
                () -> {
                    deleteDelivery(id);
                    return (Void) null;
                },
                ignored -> cachedDeliveries.removeIf(
                        delivery -> delivery.id() == id
                ),
                "Cannot delete and uncache delivery with id=" + id
        );
    }

    /**
     * Выполняет запрос в DB-потоке, затем обновляет кэш
     * в серверном потоке.
     *
     * Постановка обновления кэша происходит внутри DB-задачи:
     * до начала следующего запроса этого executor.
     *
     * Future завершается только после выполнения обновления кэша.
     */
    private static <T> CompletableFuture<T> executeAndUpdateCache(
            Supplier<T> databaseOperation,
            Consumer<T> cacheOperation,
            String errorMessage
    ) {
        CompletableFuture<T> result = new CompletableFuture<>();

        try {
            DatabaseExecutor.executor().execute(() -> {
                try {
                    T value = databaseOperation.get();

                    ServerContextHolder.executeInServerThread(() -> {
                        try {
                            cacheOperation.accept(value);
                            result.complete(value);
                        } catch (Exception e) {
                            result.completeExceptionally(e);
                        }
                    });
                } catch (Exception e) {
                    result.completeExceptionally(e);
                }
            });
        } catch (Exception e) {
            result.completeExceptionally(e);
        }

        return result.whenComplete((value, error) -> {
            if (error != null) {
                KartonDeliveryAPI.LOGGER.error(errorMessage, error);
            }
        });
    }
}