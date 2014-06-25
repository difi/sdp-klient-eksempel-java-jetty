package no.digipost.sdp;

import no.difi.sdp.client.domain.Forsendelse;
import no.difi.sdp.client.domain.kvittering.ForretningsKvittering;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Ekstremt forenklet håndtering av status på sending av digital post.
 *
 * I en reell sender vil dette være en nøkkelkomponent som håndterer alt rundt resultatet av sending av digital post.
 * Dette vil typisk inkludere oppdatering av status i database/fagsystem, automatisk feilhåndtering, rapportering til
 * manuell feilhåndtering og så videre.
 *
 * Se <a href="http://begrep.difi.no/SikkerDigitalPost/forretningslag/avsender_tilstanddiagram">mulig tilstandsdiagram</a> for forsendelser.
 */
public class SDPStatus {

    private final HashMap<String, String> status;
    private final LinkedBlockingQueue<Runnable> queue;

    public SDPStatus() {
        status = new LinkedHashMap<>();
        queue = new LinkedBlockingQueue<>(100);
    }

    public LinkedBlockingQueue<Runnable> getQueue() {
        return queue;
    }

    /**
     * Kunne ikke sendes fordi sende-køa er full. Kan tyde på at det produseres brev med for høy hyppighet, eller at
     * nettverksproblemer eller annen feil gjør at brev ikke kan sendes.
     *
     * Forsendelsen bør forsøkes på nytt senere.
     */
    public void notSentDueToCapacity(Forsendelse forsendelse) {
        status.put(forsendelse.getKonversasjonsId(), "NotSentCapacity");
    }

    /**
     * Forsendelsen er lagt i sende-køa.
     */
    public void addedToQueue(Forsendelse forsendelse) {
        status.put(forsendelse.getKonversasjonsId(), "AddedToQueue");
    }

    /**
     * Forsendelsen har blitt sendt til meldingsformidleren. Dersom det går urimelig lang tid før det mottas forretningskvittering
     * på forsendelsen bør det håndteres i henhold til avtalen med sentralforvalter, som beskrevet i
     * <a href="http://begrep.difi.no/SikkerDigitalPost/feilhandtering/Forretningsfeil">begrepskatalogen</a>.
     */
    public void sent(Forsendelse forsendelse) {
        status.put(forsendelse.getKonversasjonsId(), "Sent");
    }

    /**
     * Sending til meldingsformidleren feilet. Exception bør undersøkes for å finne ut om forsendelsen skal prøves igjen automatisk
     * eller markeres for manuell inspeksjon/feilhåndtering. Vil i praksis (nesten) alltid være en subklasse av {@link no.difi.sdp.client.domain.exceptions.SikkerDigitalPostException}.
     *
     * @see no.difi.sdp.client.domain.exceptions.SendException#getAntattSkyldig()
     */
    public void feilet(Forsendelse forsendelse, Exception e) {
        status.put(forsendelse.getKonversasjonsId(), String.format("Exception: %s (%s)", e.getClass().getSimpleName(), e.getMessage()));
    }

    /**
     * <a href="http://begrep.difi.no/SikkerDigitalPost/meldinger/"Forretningskvittering</a> for en forsendelse. Det bør
     * sjekkes hvilken subklasse av {@link no.difi.sdp.client.domain.kvittering.ForretningsKvittering} dette er og håndteres deretter.
     * Håndteringen avhenger av avsenderens krav.
     */
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
