package no.digipost.sdp.send;

import no.difi.sdp.client.SikkerDigitalPostKlient;
import no.difi.sdp.client.domain.Forsendelse;
import no.digipost.sdp.SDPStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SendDigitalPost implements Runnable {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final SikkerDigitalPostKlient klient;
    private final SDPStatus sdpStatus;
    private final Forsendelse forsendelse;

    public SendDigitalPost(SikkerDigitalPostKlient klient, SDPStatus sdpStatus, Forsendelse forsendelse) {
        this.klient = klient;
        this.sdpStatus = sdpStatus;
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
            sdpStatus.sent(forsendelse);
        }
        catch (Exception e) {
            sdpStatus.feilet(forsendelse, e);
        }
    }
}
