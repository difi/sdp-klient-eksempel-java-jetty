package no.digipost.sdp;

import no.difi.sdp.client.KlientKonfigurasjon;
import no.difi.sdp.client.SikkerDigitalPostKlient;
import no.difi.sdp.client.domain.Avsender;
import no.difi.sdp.client.domain.Noekkelpar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.KeyStore;

public class SendBrevService {

    private static final String MELDINGSFORMIDLER_URI = "https://qaoffentlig.meldingsformidler.digipost.no/api/ebms";
    private static final String AVSENDER_ORGNUMMER = "991825827";

    private final SikkerDigitalPostKlient klient;
    private final Forsendelseskilde forsendelseskilde;
    private final SendBrevStatus sendBrevStatus;

    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private BrevProdusent brevProdusent;

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

        klient = new SikkerDigitalPostKlient(Avsender.builder(AVSENDER_ORGNUMMER, noekkelpar).build(), KlientKonfigurasjon.builder().meldingsformidlerRoot(MELDINGSFORMIDLER_URI).build());

        forsendelseskilde = new Forsendelseskilde();
        sendBrevStatus = new SendBrevStatus();
        brevProdusent = new BrevProdusent(forsendelseskilde, klient, sendBrevStatus);

        // Alltid lytt på kvitteringer
        new Thread(new HentKvittering(klient, sendBrevStatus), "ReceiptPollingThread").start();
    }

    public String getStatus() {
        return this.sendBrevStatus.getStatusString();
    }

    public String getQueueStatus() {
        return this.sendBrevStatus.getQueueStatusString();
    }

    public void startSending(Integer sendIntervalMs) {
        brevProdusent.setSendInterval(sendIntervalMs);

        if (!brevProdusent.isRunning()) {
            new Thread(brevProdusent, "LetterProducer").start();
        }
    }

    public void stopSending() {
        brevProdusent.stop();
    }
}
