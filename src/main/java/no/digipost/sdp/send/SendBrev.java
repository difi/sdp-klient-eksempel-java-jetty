package no.digipost.sdp.send;

import no.difi.sdp.client.SikkerDigitalPostKlient;
import no.difi.sdp.client.domain.Forsendelse;
import no.digipost.sdp.SendBrevStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SendBrev implements Runnable {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final SikkerDigitalPostKlient klient;
    private final SendBrevStatus sendBrevStatus;
    private final Forsendelse forsendelse;

    public SendBrev(SikkerDigitalPostKlient klient, SendBrevStatus sendBrevStatus, Forsendelse forsendelse) {
        this.klient = klient;
        this.sendBrevStatus = sendBrevStatus;
        this.forsendelse = forsendelse;
    }

    @Override
    public String toString() {
        return "Send " + forsendelse.getKonversasjonsId();
    }

    @Override
    public void run() {
        log.info("Sender brev med id " + forsendelse.getKonversasjonsId());
        try {
            klient.send(forsendelse);
            sendBrevStatus.sent(forsendelse);
        }
        catch (Exception e) {
            sendBrevStatus.feilet(forsendelse, e);
        }
    }
}
