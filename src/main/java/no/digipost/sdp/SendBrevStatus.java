package no.digipost.sdp;

import no.difi.sdp.client.domain.Forsendelse;
import no.difi.sdp.client.domain.kvittering.ForretningsKvittering;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

public class SendBrevStatus {

    private final HashMap<String, String> status;
    private LinkedBlockingQueue<Runnable> queue;

    public SendBrevStatus() {
        status = new LinkedHashMap<>();
    }

    public void sendQueue(LinkedBlockingQueue<Runnable> queue) {
        this.queue = queue;
    }

    public void notSentDueToCapacity(Forsendelse forsendelse) {
        status.put(forsendelse.getKonversasjonsId(), "NotSentCapacity");
    }

    public void addedToQueue(Forsendelse forsendelse) {
        status.put(forsendelse.getKonversasjonsId(), "AddedToQueue");
    }

    public void sent(Forsendelse forsendelse) {
        status.put(forsendelse.getKonversasjonsId(), "Sent");
    }

    public void feilet(Forsendelse forsendelse, Exception e) {
        status.put(forsendelse.getKonversasjonsId(), String.format("Exception: %s (%s)", e.getClass().getSimpleName(), e.getMessage()));
    }

    public void kvittering(ForretningsKvittering forretningsKvittering) {
        status.put(forretningsKvittering.getKonversasjonsId(), forretningsKvittering.toString());
    }

    public String getStatusString() {
        StringBuilder stringBuilder = new StringBuilder();
        for (Map.Entry<String, String> entry : status.entrySet()) {
            stringBuilder.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }

        return stringBuilder.toString();
    }

    public String getQueueStatusString() {
        StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Number of elements in queue: ").append(this.queue.size()).append("\n")
                             .append("Remaining capacity: ")         .append(this.queue.remainingCapacity()).append("\n\n");

        for (Runnable runnable : queue) {
            stringBuilder.append(runnable).append("\n");
        }

        return stringBuilder.toString();
    }

}
