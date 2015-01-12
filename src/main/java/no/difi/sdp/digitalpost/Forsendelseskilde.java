package no.difi.sdp.digitalpost;

import no.difi.begrep.Person;
import no.difi.kontaktinfo.wsdl.oppslagstjeneste_14_05.Oppslagstjeneste1405;
import no.difi.kontaktinfo.xsd.oppslagstjeneste._14_05.HentPersonerForespoersel;
import no.difi.kontaktinfo.xsd.oppslagstjeneste._14_05.HentPersonerRespons;
import no.difi.kontaktinfo.xsd.oppslagstjeneste._14_05.Informasjonsbehov;
import no.difi.sdp.client.domain.*;
import no.difi.sdp.client.domain.digital_post.DigitalPost;

import java.io.File;
import java.util.Arrays;
import java.util.UUID;

import static no.difi.sdp.client.domain.digital_post.Sikkerhetsnivaa.NIVAA_3;

public class Forsendelseskilde {

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
        String konversasjonsId = UUID.randomUUID().toString();

        Mottaker mottaker = hentMottaker("04036125433");

        Behandlingsansvarlig behandlingsansvarlig = Behandlingsansvarlig.builder("991825827").avsenderIdentifikator("Difi testavsender").build();
        DigitalPost testpost = DigitalPost.builder(mottaker, "Her er et testbrev!").sikkerhetsnivaa(NIVAA_3).build();
        Dokumentpakke dokumentpakke = Dokumentpakke.builder(Dokument.builder("Her er innholdet", new File("testbrev.pdf")).build()).build();

        return Forsendelse.digital(behandlingsansvarlig, testpost, dokumentpakke).konversasjonsId(konversasjonsId).build();
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
            throw new PersonNotFoundException();
        }

        Person person = personRespons.getPerson().get(0);
        String postkasseadresse = person.getSikkerDigitalPostAdresse().getPostkasseadresse();
        Sertifikat sertifikat = Sertifikat.fraByteArray(person.getX509Sertifikat());
        String orgnummerPostkasse = person.getSikkerDigitalPostAdresse().getPostkasseleverandoerAdresse();

        return Mottaker.builder(personidentifikator, postkasseadresse, sertifikat, orgnummerPostkasse).build();
    }

}
