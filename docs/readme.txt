Link al corso https://www.udemy.com/course/java-messaging-service-spring-mvc-spring-boot-activemq/

Intro
	Apache ActiveMQ è un message broker open source (es. whatsapp usa al suo interno un message broker per far scambiare i msg tra utenti)
Intro a JMS (Java Messaging Service)
	In questa sezione implementeremo un JMS usando Spring MVC
	Che cos'e' JMS
		e' un servizio che permette creare una comunicazione tra le app java 
		vengono scambiati i messaggi senza una comunicazione diretta tra le app
		come intermediario in questa comunicazione abbiamo una QUEUE (coda)
			una coda riceve i messaggi dall'app e invia i msg ricevuti ai recevier (ricevitori che devono ricevere il messaggio) che a sua volta sono delle app java
			nel nostro caso per la coda useremo ActiveMQ dove andremmo a creare le nostre code
			un sender e' un message publisher, chi pubblica il messaggio
			un receiver e' un message comsumer, chi consuma il messaggio
	Spring fornisce JMS template per poter usare JMS all'interno di spring framework
	Possiamo avere un sendere e N receiver
		NOTA: solo un receiver (aka consumer) riceve un messaggio dalla coda, praticamente un messaggio viene consumato SOLO UNA VOLTA
		la logica lato consumer - per es. se il consumer e' responsabile dell'invio della mail, la inviera'
		nel nostro esempio faremo due app per il publisher e consumer
Primo contatto con Apache ActiveMQ
	Che cos'e' una coda in ActiveMQ
		coda riceve e persiste i msg 
		il suo compito e' far arrivare i msg al consumer giusto (se il consumer e' offline il msg rimane in coda finche non viene scodato)
		la coda ha un concetto di acknowledgment, la garanzia che un msg e' stato ricevuto dal consumer (vedi sotto per dettagli)
		NOTA: anche RabbitMQ e' un message broker
		quando abbiamo N consumer, la coda applica algoritmo Round Robit nel passaggio di messaggi, ordine sequenziale..
	Che cos'e' un topic in ActiveMQ
		es. gruppi whatsapp, una persona scrive nel gruppo, tutti i membri ricevono il messaggio 
		un messaggio viene ricevuto da tutti i consumer che si sono sottoscritti al topic 
	Installazione Apache ActiveMQ
		link https://activemq.apache.org/components/artemis/
		nella cartella bin troviamo l'eseguibile in base al SO
		di default ActiveMQ parte sulla porta 8161
		la pagina di gestione di default e' accessibile usando le credenziali admin:admin
			pagina queue -> creiamo nuova coda impostando il nome
			(se abbiamo una coda con solo publisher, i msg accodati saranno in stato pending)
Autenticazione e autorizzazione
	prima di usare una coda dobbiamo configurare autorizzazione e autenticazione
	la connessione tra app e ActiveMQ avviene usando il broker URL e connessione TCP
	di default tutti possano pubblicare e leggere i msg 
	le credenziali usati per la web console sono differenti da quelli usati per lo scambio di msg
	la guida del corso e' relativa ad una versione precedente di ActiveMQ, consultare il manuale della versione in uso per capire in che modo possiamo aggiungere utenti e ruoli
		cmq possiamo creare utenti e successivamente configurare accessi alle code 
		un utente puo' accedere a tutte le code o ad un insieme limitato di code, puo' accedere solo per pubblicare o leggere o entrambe le modalita'
		se ho capito bene la configurazione di accessi alle code sono per ruoli
Primo contatto con le code
	creiamo progetto maven
	aggiungiamo dipendenza al activemq-broker nel pom.xml (verificare la versione che sia compatibile con il broker in esecuzione)
	pubblicazione di un messaggio da java
		creiamo un main() -> creiamo factory della connessione passando user:pwd:brokerurl (es. admin, admin, tcp://localhost:61616) -> creiamo la connessione usando la factory 
			-> creiamo la sessione partendo dalla connection, passando il flag per la transazionalita' e acknowledgeMode (auto/client, vedi sotto per dettagli) -> 
			-> creiamo la destinazione (es. coda) partendo dalla sessione (passiamo il nome della coda) -> NOTA: se la coda non esiste, viene creata al volo -> 
			-> creiamo un msg testuale da pubblciare usando l'oggetto della sessione -> creiamo il producer per la coda usando l'oggetto della sessione e passiamo la destinazione come parametro 
			(praticamente stiamo creando un producer dedicato alla nostra coda)
			-> usiamo il producer per pubblicare il messaggio (es. producer.send(msg))
			-> alla fine chiudiamo la sessione e connessione - FINE!
			dalla console possiamo vedere il messaggio pubblicato
	consumazione del messaggio da java
		creiamo un main() -> creiamo factory della connessione passando user:pwd:brokerurl -> creiamo la connessione usando la factory 
			-> facciamo partire la connessione chiamando il metodo start() (NOTA: serve per consumer!)
			-> creiamo la sessione partendo dalla connection NOTA: il flag acknowledgeMode puo' essere AUTO o CLIENT, nel primo caso non dobbiamo notificare la coda quando consumiamo il msg, la notifica viene inviata in automatico
				invece nel caso di CLIENT, dobbiamo notificare la coda dal nostro codice 
			-> creiamo la destinazione (es. coda) partendo dalla sessione
			-> creiamo il consumer usando l'oggetto sessione e passiamo la destinazione come parametro
			-> usiamo oggetto di consumer per creare il listener (implementiamo il metodo onMessage()), praticamente un consumer rimane sempre in ascolto sulla coda (destinazione)
				possiamo castare il messaggio se conosciamo il tipo di messaggio pubblicato (per es. TextMessage)
		dalla web console vediamo tutti consumer in ascolto e messaggi scodati
	client acknowledgment
		Session.CLIENT_ACKNOWLEDGE - il consumer deve notificare la coda esplicitamente, altrimenti il msg viene letto ma sulla coda risulta pending, e il msg rimane nella coda
		per notificare la coda, una volta che abbiamo letto il msg, possiamo chiamare il metodo textMessage.acknowledge()
		modalita' CLIENT_ACKNOWLEDGE puo' essere utile quando abbiamo una logica una volta letto il messaggio, logica in try-catch, e se l'esecuzione fallisce, lasciamo il msg in coda
	esempio di Round Robin con N consumer
		vedi demo
		due jar eseguibili che sono dei consumer 
		un producer che pubblica 4msg 
	un esempio reale di utilizzo ActiveMQ
		possiamo avere una API REST che riceve la richiesta di export in excel, pubblica un messaggio in formato JSON nella coda, che viene scodato dal consumer opportuno
		API risponde subito al client dicendo che la richiesta e' stata presa in carico e verra' inviata una mail appena export e' pronto
		la logica di elaborazione, generazione excel e invio mail e' lato consumer
	pubblicazione di un msg in formato JSON nella coda
		aggiungiamo artefatto org.json:json per lavorare con il formato json in java
		classi in gioco sono JSONObject -> aggiungiamo N attributi in base alle nostre esigenze
		json.toString() per avere la stringa da inviare nella coda
Primo contatto con topic 
	nella versione precedente di ActiveMQ e' possibile creare i topic dalla web console
	nel contesto di topic parliamo di subscriber, tutti quelli che vogliono riceve i msg di un topic
	pubblicazione del messaggio nel topic
		creiamo un main() -> creiamo factory della connessione passando user:pwd:brokerurl -> creiamo la connessione usando la factory 
		-> creiamo la sessione partendo dalla connection
		-> creiamo la destinazione (topic) partendo dalla sessione
		-> creiamo il producer per il topic usando l'oggetto della sessione e passiamo la destinazione come parametro 
		-> creiamo un msg testuale da pubblciare usando l'oggetto della sessione
		-> usiamo il producer per pubblicare il messaggio
		-> alla fine chiudiamo la sessione e connessione
	consumazione di un msg dal topic
		creiamo un main() -> creiamo la connection come negli esempi precedenti 
			-> la connessione va avviata subito
			-> quando creiamo la destinazione per il topic, se il topic non esiste, viene creato al volo
			NOTA: qui abbiamo il concetto di 'durable subscriber', praticamente quando un subscriber e' offline e il topic riceve nuovi msg, anche se abbiamo altri subscriber online che 
				continuano a leggere i msg, quando il subscriber offline ritorna online, lui cmq leggera' tutti i msg arrivati nel topic mentre era' offline.
			-> creiamo consumer partendo dalla sessione (es. session.createDurableSubscriber(topic, 'consumer name'))
			NOTA: quando creiamo durable subscriber dobbiamo impostare il ClientID della Connection, serve a ActiveMQ per capire i msg non letti dal durable subscriber mentre era' offline
			-> configuriamo il listener del consumer
	topic con piu' consumer
ActiveMQ con Spring MVC
	activemq-spring, spring-jms - due dipendenze da aggiungere nel pom.xml
	in questo esempio i publisher e subscriber sono delle app spring mvc
	producer - invia un msg in JSON contenente i dati del prodotto nella coda
		classe JmsConfig, contiene la creazione della factory, creazione di JmsTemplate contenente la factory della connection
		classe MessagePublisher marcata come @Component, metodo x inviare il msg (in una coda), iniettiamo JmsTemplate, usiamo metodo send() di JmsTemplate passando la coda e il msg creator
		a livello di Controller MVC inviettiamo il MessagePublisher per inviare il msg 
	consumer - legge il msg usando JmsTemplate
		classe JmsConfig e' identica a quella di producer, aggiungiamo solo il metodo che ritorna il listener (DefaultJmsListenerContainerFactory)
		classe MessageConsumer marcata come @Component, metodo x ricevere il msg, il metodo che riceve il msg e' marcato con @JmsListener(destination = "nome della queue")
		ObjectMapper di fasterxml.jackson puo' essere usato per creare un oggetto partendo dalla stringa JSON
ActiveMQ con Spring Boot
	Spring JMS template viene usato per pubblicare e ricevere i msg
	in questo esempio le entrambe app sono delle app spring boot
	setup del progetto per JMS
		aggiungiamo dipendenze nel pom.xml spring-boot-starter-activemq, activemq-broker
		il metodo main e' quello di spring boot
		application.properties contiene le props di ActiveMQ (brokerUrl, username, pwd)
			NOTA: non dobbiamo creare nessun altro bean come nel caso di Spring MVC
	pubblicazione di un messaggio
		creiamo SendController marcato con @RestController, @RequestMapping("api/v1"), metodo send(msg) marcato @GetMapping (vedi demo)
		iniettiamo nella classe SendController JmsTemplate
		usando JmsTemplate inviamo il msg, vedi demo
		lanciamo Spring Boot, NOTA: in questo caso parte anche il tomcat che ospitera' la nostra API REST
		digitando URL nel browser inviamo il msg
	consumazione del messaggio
		la config non cambia rispetto il publisher
		creiamo MessageConsumer marcato @Component, metodo x ricevere il msg marcato @JmsListener, 

		
		