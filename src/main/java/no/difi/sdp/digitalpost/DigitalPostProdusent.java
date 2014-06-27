package no.difi.sdp.digitalpost;

import no.difi.sdp.SDPStatus;
import no.difi.sdp.client.SikkerDigitalPostKlient;
import no.difi.sdp.client.domain.Forsendelse;
import no.difi.sdp.send.SendDigitalPost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Putter jobber i en kø for sending. Bør ha mekanismer for å sørge for å ikke produsere brev fortere enn de sendes.
 */
public class DigitalPostProdusent implements Runnable {

    private final Forsendelseskilde forsendelseskilde;
    private final SikkerDigitalPostKlient klient;
    private final SDPStatus sdpStatus;

    private int sendInterval = 1000;
    private boolean running = false;

    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private final ThreadPoolExecutor executor;
    private final LinkedBlockingQueue<Runnable> queue;

    public DigitalPostProdusent(Forsendelseskilde forsendelseskilde, SikkerDigitalPostKlient klient, SDPStatus sdpStatus) {
        this.forsendelseskilde = forsendelseskilde;
        this.klient = klient;
        this.sdpStatus = sdpStatus;

        this.queue = new LinkedBlockingQueue<>(100);
        this.executor = new ThreadPoolExecutor(1, 10, 1, TimeUnit.MINUTES, queue);
        this.sdpStatus.setQueue(queue);
    }

    @Override
    public void run() {
        if (running) {
            throw new IllegalStateException("Attempted to start already running " + this.getClass().getSimpleName());
        }

        running = true;
        while (running) {
            // Hent forsendelse
            try {
                Forsendelse forsendelse = forsendelseskilde.lagBrev();

                if (queue.remainingCapacity() == 0) {
                    // Håndter eventuelt full kø
                    sdpStatus.notSentDueToCapacity(forsendelse);
                    log.warn("[" + forsendelse.getKonversasjonsId() + "] not sent due to full queue");
                } else {
                    // Legg brev til sending
                    executor.submit(new SendDigitalPost(klient, sdpStatus, forsendelse));
                    sdpStatus.addedToQueue(forsendelse);
                    log.info("[" + forsendelse.getKonversasjonsId() + "] added to send queue");
                }
            }
            catch (PersonNotFoundException e) {
                // Hvis en person ikke finnes i kontaktregisteret eller kontaktregisteret ikke gir et gyldig svar må det gjøre alternativ håndtering, typisk print.
                log.warn("Kunne ikke lage digital post til mottakeren: mottakeren er ikke registrert i kontaktregisteret");
            }

            try {
                Thread.sleep(sendInterval);
            } catch (InterruptedException e) {
                log.warn("Awoken from sleep. Jobs will arrive quickly!");
            }
        }
    }

    public void setSendInterval(Integer sendInterval) {
        this.sendInterval = sendInterval;
    }

    public boolean isRunning() {
        return running;
    }

    public void stop() {
        running = false;
    }
}
