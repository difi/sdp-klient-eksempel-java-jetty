package no.digipost.sdp.digitalpost;

import no.difi.sdp.client.SikkerDigitalPostKlient;
import no.difi.sdp.client.domain.Forsendelse;
import no.digipost.sdp.SDPStatus;
import no.digipost.sdp.send.SendDigitalPost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    public DigitalPostProdusent(Forsendelseskilde forsendelseskilde, SikkerDigitalPostKlient klient, SDPStatus sdpStatus) {
        this.forsendelseskilde = forsendelseskilde;
        this.klient = klient;
        this.sdpStatus = sdpStatus;

        executor = new ThreadPoolExecutor(1, 10, 1, TimeUnit.MINUTES, sdpStatus.getQueue());
    }

    @Override
    public void run() {
        if (running) {
            throw new IllegalStateException("Attempted to start already running " + this.getClass().getSimpleName());
        }

        running = true;
        while(running) {
            // Hent forsendelse
            Forsendelse forsendelse = forsendelseskilde.lagBrev();

            if (sdpStatus.getQueue().remainingCapacity() == 0) {
                // Håndter eventuelt full kø
                sdpStatus.notSentDueToCapacity(forsendelse);
                log.warn("[" + forsendelse.getKonversasjonsId() + "] not sent due to full queue");
            }
            else {
                // Legg brev til sending
                sdpStatus.addedToQueue(forsendelse);
                log.info("[" + forsendelse.getKonversasjonsId() + "] added to send queue");

                executor.submit(new SendDigitalPost(klient, sdpStatus, forsendelse));
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
