package no.digipost.sdp;

import no.difi.sdp.client.SikkerDigitalPostKlient;
import no.difi.sdp.client.domain.Prioritet;
import no.difi.sdp.client.domain.kvittering.ForretningsKvittering;
import no.difi.sdp.client.domain.kvittering.KvitteringForespoersel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class HentKvittering implements Runnable {

    public static final int MILLIS_BETWEEN_RECEIPT_CHECKS_WHEN_EMPTY = (int) TimeUnit.MINUTES.toMillis(10);

    /**
     * Hvilken MPC-kø vi skal lytte på kvitteringer fra.
     *
     * Hvis det gjøres forsendelser med forskjellig prioritet må det lyttes på begge køene.
     */
    public static final Prioritet MPC_PRIORITY = Prioritet.NORMAL;

    private final SikkerDigitalPostKlient klient;
    private final SendBrevStatus sendBrevStatus;

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    public HentKvittering(SikkerDigitalPostKlient klient, SendBrevStatus sendBrevStatus) {
        this.klient = klient;
        this.sendBrevStatus = sendBrevStatus;
    }

    @Override
    public void run() {
        ForretningsKvittering forrigeKvittering = null;
        while(true) {
            try {
                forrigeKvittering = hentKvitteringOgBekreftForrige(forrigeKvittering);
            }
            catch (RuntimeException e) {
                log.error("Caught Exception while trying to fetch receipt", e);
            }
        }
    }

    private ForretningsKvittering hentKvitteringOgBekreftForrige(ForretningsKvittering forrigeKvittering) {
        ForretningsKvittering forretningsKvittering = klient.hentKvitteringOgBekreftForrige(KvitteringForespoersel.builder(MPC_PRIORITY).build(), forrigeKvittering);

        if (forretningsKvittering != null) {
            sendBrevStatus.kvittering(forretningsKvittering);
        }
        else {
            try {
                log.info("No receipt. Being a good lad and sleeping " + MILLIS_BETWEEN_RECEIPT_CHECKS_WHEN_EMPTY + "ms");
                Thread.sleep(MILLIS_BETWEEN_RECEIPT_CHECKS_WHEN_EMPTY);
            } catch (InterruptedException e) {
                log.warn("Interrupted from sleep. Will poll for new receipt sooner than intended");
            }
        }

        return forretningsKvittering;
    }
}
