import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

/*
Rastreia todas as mensagens assincronas. É notificado toda vez que um ack é recebido de um membro.
 */
public class ResultsCollector {
    int totalAcks;
    int receivedAcks;
    CompletableFuture future = new CompletableFuture();

    public ResultsCollector(int totalAcks) {
        this.totalAcks = totalAcks;
    }

    public void acksReceived() {
        receivedAcks++;
        if (receivedAcks == totalAcks) {
            future.complete(true);
        }
    }

    public void orTimeout(int time, TimeUnit unit){
        future.orTimeout(time, unit);
    }

    public void whenComplete(BiConsumer<? super Object, ? super Throwable> func) {
        future.whenComplete(func);
    }

    public void complete() {
        future.complete(true);
    }
}
