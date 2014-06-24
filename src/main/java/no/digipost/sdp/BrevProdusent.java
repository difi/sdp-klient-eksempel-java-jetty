package no.digipost.sdp;

import no.difi.sdp.client.SikkerDigitalPostKlient;
import no.difi.sdp.client.domain.Forsendelse;
import no.digipost.sdp.send.SendDigitalPost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Putter jobber i en kø for sending. Bør ha mekanismer for å sørge for å ikke produsere brev fortere enn de sendes.
 */
public class BrevProdusent implements Runnable {

    private final Forsendelseskilde forsendelseskilde;
    private final SikkerDigitalPostKlient klient;
    private final SendBrevStatus sendBrevStatus;
    private final LinkedBlockingQueue<Runnable> workQueue;

    private int sendInterval = 1000;
    private boolean running = false;

    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private final ThreadPoolExecutor executor;

    public BrevProdusent(Forsendelseskilde forsendelseskilde, SikkerDigitalPostKlient klient, SendBrevStatus sendBrevStatus) {
        this.forsendelseskilde = forsendelseskilde;
        this.klient = klient;
        this.sendBrevStatus = sendBrevStatus;

        workQueue = new LinkedBlockingQueue<>(100);
        executor = new ThreadPoolExecutor(1, 10, 1, TimeUnit.MINUTES, workQueue);

        sendBrevStatus.sendQueue(workQueue);
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

            if (workQueue.remainingCapacity() == 0) {
                // Håndter eventuelt full kø
                sendBrevStatus.notSentDueToCapacity(forsendelse);
            }
            else {
                // Legg brev til sending
                executor.submit(new SendDigitalPost(klient, sendBrevStatus, forsendelse));
                sendBrevStatus.addedToQueue(forsendelse);
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
