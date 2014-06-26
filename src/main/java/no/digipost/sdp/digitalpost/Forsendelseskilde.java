package no.digipost.sdp.digitalpost;

import no.difi.begrep.Person;
import no.difi.kontaktinfo.wsdl.oppslagstjeneste_14_05.Oppslagstjeneste1405;
import no.difi.kontaktinfo.xsd.oppslagstjeneste._14_05.HentPersonerForespoersel;
import no.difi.kontaktinfo.xsd.oppslagstjeneste._14_05.HentPersonerRespons;
import no.difi.kontaktinfo.xsd.oppslagstjeneste._14_05.Informasjonsbehov;
import no.difi.sdp.client.domain.Behandlingsansvarlig;
import no.difi.sdp.client.domain.Dokument;
import no.difi.sdp.client.domain.Dokumentpakke;
import no.difi.sdp.client.domain.Forsendelse;
import no.difi.sdp.client.domain.Mottaker;
import no.difi.sdp.client.domain.Sertifikat;
import no.difi.sdp.client.domain.digital_post.DigitalPost;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Arrays;

import static no.difi.sdp.client.domain.digital_post.Sikkerhetsnivaa.NIVAA_3;

public class Forsendelseskilde {

    private static Logger LOG = LoggerFactory.getLogger(Forsendelseskilde.class);

    private final Oppslagstjeneste1405 kontaktinfoPort;

    public Forsendelseskilde(Oppslagstjeneste1405 kontaktinfoPort) {
        this.kontaktinfoPort = kontaktinfoPort;
    }

    /**
     * Grovt forenklet produksjon av forsendelser. I praksis vil dette være å hente ut brev som skal sendes fra et fagsystem,
     * slå opp mottakeren mot <a href="http://begrep.difi.no/Oppslagstjenesten/">kontakt og reservasjonsregisteret</a>
     * og bygge forsendelsen.
     */
    public Forsendelse lagBrev() {
        String konversasjonsId = "konversasjonsid-" + new DateTime().toString("yyyyMMdd-HHmmssSSS");

        Mottaker mottaker = hentMottaker("04036125433");

        if (mottaker != null) {
            Behandlingsansvarlig behandlingsansvarlig = Behandlingsansvarlig.builder("991825827").avsenderIdentifikator("Difi testavsender").build();
            DigitalPost testpost = DigitalPost.builder(mottaker, "Her er et testbrev!").sikkerhetsnivaa(NIVAA_3).build();
            Dokumentpakke dokumentpakke = Dokumentpakke.builder(Dokument.builder("Her er innholdet", new File("testbrev.pdf")).build()).build();

            return Forsendelse.digital(behandlingsansvarlig, testpost, dokumentpakke).konversasjonsId(konversasjonsId).build();
        }

        return null;
    }

    /**
     * Henter mottaker fra kontaktregisteret.
     */
    private Mottaker hentMottaker(String personidentifikator) {
        HentPersonerForespoersel personForespoersel = new HentPersonerForespoersel();
        personForespoersel.getInformasjonsbehov().addAll(Arrays.asList(Informasjonsbehov.SERTIFIKAT, Informasjonsbehov.SIKKER_DIGITAL_POST));
        personForespoersel.getPersonidentifikator().add(personidentifikator);

        HentPersonerRespons personRespons = kontaktinfoPort.hentPersoner(personForespoersel);

        if (personRespons.getPerson().isEmpty()) {
            LOG.error("Fant ikke personen [" + personidentifikator + "] i oppslagstjenesten, kan ikke lage forsendelse for denne personen.");
            return null;
        }

        Person person = personRespons.getPerson().get(0);
        String postkasseadresse = person.getSikkerDigitalPostAdresse().getPostkasseadresse();
        Sertifikat sertifikat = Sertifikat.fraByteArray(person.getX509Sertifikat());
        String orgnummerPostkasse = person.getSikkerDigitalPostAdresse().getPostkasseleverandoerAdresse();

        return Mottaker.builder(personidentifikator, postkasseadresse, sertifikat, orgnummerPostkasse).build();
    }

}
