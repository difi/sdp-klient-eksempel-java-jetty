package no.digipost.sdp;

import no.difi.sdp.client.KlientKonfigurasjon;
import no.difi.sdp.client.SikkerDigitalPostKlient;
import no.difi.sdp.client.domain.Noekkelpar;
import no.difi.sdp.client.domain.TekniskAvsender;
import no.digipost.sdp.digitalpost.DigitalPostProdusent;
import no.digipost.sdp.digitalpost.Forsendelseskilde;
import no.digipost.sdp.send.HentKvittering;

import java.security.KeyStore;

public class SDPService {

    private static final String MELDINGSFORMIDLER_URI = "https://qaoffentlig.meldingsformidler.digipost.no/api/ebms";
    private static final String AVSENDER_ORGNUMMER = "991825827";

    private final SikkerDigitalPostKlient klient;
    private final Forsendelseskilde forsendelseskilde;
    private final SDPStatus sdpStatus;
    private final Thread kvitteringThread;

    private DigitalPostProdusent digitalPostProdusent;

    public SDPService() {
        Noekkelpar noekkelpar;
        try {
            KeyStore keyStore = KeyStore.getInstance("JCEKS");

            // Last keystore som inneholder sertifikatkjede og privatnøkkel for avsender, samt eventuelt trust store for rotsertifikat brukt av meldingsformidler.
            keyStore.load(this.getClass().getClassLoader().getResourceAsStream("./keystore.jce"), "abcd1234".toCharArray());
            noekkelpar = Noekkelpar.fraKeyStore(keyStore, "meldingsformidler", "abcd1234");
        }
        catch (Exception e) {
            throw new RuntimeException("Unable to init keystore", e);
        }

        klient = new SikkerDigitalPostKlient(TekniskAvsender.builder(AVSENDER_ORGNUMMER, noekkelpar).build(), KlientKonfigurasjon.builder().meldingsformidlerRoot(MELDINGSFORMIDLER_URI).build());

        forsendelseskilde = new Forsendelseskilde();
        sdpStatus = new SDPStatus();
        digitalPostProdusent = new DigitalPostProdusent(forsendelseskilde, klient, sdpStatus);

        // Alltid lytt på kvitteringer
        kvitteringThread = new Thread(new HentKvittering(klient, sdpStatus), "ReceiptPollingThread");
        kvitteringThread.start();
    }

    public void startSending(Integer sendIntervalMs) {
        digitalPostProdusent.setSendInterval(sendIntervalMs);

        if (!digitalPostProdusent.isRunning()) {
            new Thread(digitalPostProdusent, "LetterProducer").start();
        }
    }

    public void stopSending() {
        digitalPostProdusent.stop();
    }

    public void pullReceipt() {
        kvitteringThread.interrupt();
    }

    public String getStatus() {
        return this.sdpStatus.getStatusString();
    }

    public String getQueueStatus() {
        return this.sdpStatus.getQueueStatusString();
    }
}
