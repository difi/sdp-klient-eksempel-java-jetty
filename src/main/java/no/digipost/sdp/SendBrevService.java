package no.digipost.sdp;

import no.difi.sdp.client.KlientKonfigurasjon;
import no.difi.sdp.client.SikkerDigitalPostKlient;
import no.difi.sdp.client.domain.Avsender;
import no.difi.sdp.client.domain.Noekkelpar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.KeyStore;

public class SendBrevService {

    private final SikkerDigitalPostKlient klient;
    private final Forsendelseskilde forsendelseskilde;
    private final SendBrevStatus sendBrevStatus;

    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private BrevSender brevSender;

    public SendBrevService() {
        Noekkelpar noekkelpar;
        try {
            KeyStore keyStore = KeyStore.getInstance("JCEKS");
            keyStore.load(this.getClass().getClassLoader().getResourceAsStream("./keystore.jce"), "abcd1234".toCharArray());
            noekkelpar = Noekkelpar.fraKeyStore(keyStore, "meldingsformidler", "abcd1234");
        }
        catch (Exception e) {
            throw new RuntimeException("Unable to init keystore", e);
        }

        klient = new SikkerDigitalPostKlient(Avsender.builder("991825827", noekkelpar).build(), KlientKonfigurasjon.builder().meldingsformidlerRoot("https://qaoffentlig.meldingsformidler.digipost.no/api/ebms").build());

        forsendelseskilde = new Forsendelseskilde();
        sendBrevStatus = new SendBrevStatus();
        brevSender = new BrevSender(forsendelseskilde, klient, sendBrevStatus);
    }

    public void start() {
        log.info("Starter kvitteringshenter");
        new Thread(new HentKvittering(klient, sendBrevStatus)).start();
    }

    public String getStatus() {
        return this.sendBrevStatus.getStatusString();
    }

    public String getQueueStatus() {
        return this.sendBrevStatus.getQueueStatusString();
    }

    public void startSending(Integer sendIntervalMs) {
        brevSender.setSendInterval(sendIntervalMs);

        if (!brevSender.isRunning()) {
            new Thread(brevSender).start();
        }
    }

    public void stopSending() {
        brevSender.stop();
    }
}
