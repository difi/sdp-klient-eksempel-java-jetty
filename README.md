Sikker Digital Post-sender
==========================

Eksempelapplikasjon for sending av sikker digital post. Denne er lagd for å vise hvordan <a href="https://github.com/difi/sikker-digital-post-java-klient">Sikker Digital Post Java-klient</a> 
kan brukes av en avsender i det offentlige.

Arkitektur
----------

Grensesnittet for sending av digital post er asynkront i natur og krever en håndtering av tråder og vedlikehold av status for sendte brev. I tillegg drar Java-klienten med seg en del transitive avhengigheter.
Det kan derfor være hensiktsmessig å gjøre sending av digital post i en egen applikasjon i miljøer der det er mulig, heller enn å bygge det inn i en eksisterende applikasjon med mange andre ansvarsområder.

Oppsett
-------

For å kunne kjøre testkoden må det konfigureres opp et keystore med et gyldig avsendersertifikat. Navn på keystore, samt alias og passord, settes i `SDPService.java`.

Eksempelapplikasjonen baserer seg på en embedded jetty-server. Serveren kan startes opp med kjøre main-metoden i `WebServerMain` eller med maven: `mvn clean compile exec:java`.

Applikasjonen starter en Jersey webapplikasjon på port 1234. Applikasjonen har noen endpoints for å gjøre det enkelt å teste sending av brev. Alle operasjonene er HTTP Get slik at de skal være enkle å kalle fra nettleseren.
Endpointet ligger i `DigitalPostResource`.

* `/start` starter sending av post. Bruk query-parameteret `interval` for å definere interval i ms. Eks: `http://localhost:1234/start?interval=2500`.
* `/stop` pauser sending av post.
* `/status` henter ut status for sendte brev.
* `/queue` viser status for køen over produserte brev som skal sendes til meldingsformidler.
* `/receipt` tvinger frem henting av kvitteringer. Normalt hentes kvitteringer hvert tiende minutt.

Struktur
--------

Applikasjonen benytter to `java.lang.Thread`-tråder i tillegg til en Thread pool. De to trådene har ansvar for:

1. Å produsere brev og legge brevene på en kø for sending. 
I denne applikasjonene bygges det et brev til en statisk mottaker basert på en enkelt fil på disk. 
I en ekte applikasjon vil dette dreie seg om å finne mottaker og brev i fagsystem (eller generere brev) og bygge request-objekter ut fra det.  
2. Hente kvitteringer og oppdatere status for tidligere sendte brev.
I en ekte applikasjon bør dette suppleres med en tråd som sjekker at man faktisk mottar kvittering for alle sendte brev. Siden mottak av brev er fullstendig asynkront kan man i enkelte avvikstilfeller risikere å hverken få suksess- eller feilkvittering for sendt brev. Dette bør da sendes til manuell håndtering.

Thread poolen har ansvar for gjennomføre sending av de produserte brevene.