In questo documento scrivo vari appunti che riguardano il message broker ActiveMQ Artemis.

Sto leggendo la documentazione fornita insieme al prodotto (disponibile cmq anche online)
link locale http://localhost:8161/user-manual/

Concetti di messaggistica
	Artemis e' un sistema di messaggistica assincrona
	Supporta due modelli: QUEUE (anycast) e TOPIC (multicast)
	Supporta vari protocolli (JMS, AMQP, MQTT, STOMP, OpenWire)
	Alta affidabilita' (HA) - le sessioni con il message broker vengono spostati dal nodo KO al nodo di backup
	Clustering - per gestire un carico di lavoro elevato, creando i cluster con piu' nodi
		i messaggi sono spostati dai nodi piu' carichi a quelli meno
	Supporta un sistema di routing e bridge
Architettura
	e' un insieme di oggetti POJO
	all'interno ha un journal persistente molto performante, usato per la messagistica e persistenza di messaggi
		questo meccanismo si rivela piu' performante a quello di JDBC
	supporta due API lato client
		1. Core client API
		2. JMS 2.0 client API
	oltre ai due client, il server Artemis supporta i seguenti protocolli di comunicazione (da usare con client dedicati)
		- AMQP
		- OpenWire
		- MQTT
		- STOMP
		- HornetQ (for use with HornetQ clients).
		- Core (Artemis CORE protocol)
	le semantiche JMS sono implementate dalla JMS facade
	NOTA: Artemis non conosce JMS, JMS facade traduce le chiamate nelle chiamate alle API client Core di Artemis
	Artemis puo' essere embeddato in una app nostra.
	Puo' essere integrato in qualisasi server JEE, supporta architettura JCA (Java Connector Architecture)
		da usare un addattore JCA opportuno per essere complient con specifiche JEE
Utilizzo del server
	per usare Artemis dobbiamo creare una sua istanza
		e' una cartella che contiene tutti i dati di runtime e la configurazione
		creare questa istanza al di fuori della cartella dove abbiamo estratto Artemis (facilita aggiornamenti futuri)
	per creare una nuova istanza lanciamo il comanda 'artemis create [...parametri di creazione...]'
	vedi la documentazione per i file di configurazione.
	il broker puo' essere avviato con il comando artemis run lanciato dalla bin di broker
	possiamo installare il broker come un servizio windows lanciando artemis-service install dalla bin di broker
Modello di indirizzamento
	il modello di indirizzamento messaggi che gestisce Artemis e' potente e flessibile con ottime prestazioni
	il modello di indirizzamento si basa sui tre concetti
		- Indirizzi (Addresses)
			un indirizzo rappresente un endpoint per la messagistica, con una sua configurazione, nome univoco, 0 o piu' code, e un tipo di routing
		- Code (Queues)
			una coda e' associata ad un indirizzo, possano esserci piu' code per un indirizzo, quando un messaggio viene associato ad un indirizzo,
			viene scelta la coda o code di destinazione in base al tipo di routing configurato. 
			le code possano essere create ed eliminate al volo.
		- Tipi di routing (Routing types)
			il routing determina in che modo i messaggi arrivano alla coda/e associate ad un indirizzo.
			in Artemis gli indirizzo possano essere configurati con due tipi di routing
				1. Anycast (point-to-point)
				2. Multicast (publish-subscribe)
			NOTA: e' consigliato configurare solo un tipo di routing per un indirizzo
			se sono stati configurati entrambi tipi, e il client non specifica una preferenza, di default viene usato il meccanismo point-to-point.
	configurazione base di un indirizzo
		messaggistica point-to-point
			indirizzo viene configurato con il routing Anycast
			se abbiamo N consumer, i messaggi sono distribuiti in modo equo, applicando alg Round Robin (i messaggi distribuiti in modo equo alle code)
			la configurazione avviene nel file broker.xml
				viene aggiunta la sezione <address> se non esiste ancora
				per convensione point-to-point il nome di indirizzo deve corrispondere al nome della coda, es:
				
				<addresses>
				   <address name="orders">
					  <anycast>
						 <queue name="orders"/>
					  </anycast>
				   </address>
				</addresses>
				
		messaggistica publish-subscribe
			in questo caso un msg viene inviato a tutti gli subscriber sottoscritti all'indirizzo (in JMS il termine usato e' TOPIC)
			esempio di creazione nuovo indirizzo
			
			<addresses>
			   <address name="pubsub.foo">
				  <multicast/>
			   </address>
			</addresses>
			
			NOTA: quando un client si connette ad un indirizzo multicast, viene creata una coda dedicato ad esso. Tale coda ricevera' tutti i msg inviata all'indirizzo. 
				e' possibile preconfigurare le code per i client e sottoscriversi direttamente ad essi. 
			
		un client puo' usare il nome completo di una coda (fully qualified queue name) per eseguire la sottoscrizione. 
		come possiamo filtrare i messaggi
			Artemis supporto espressioni per poter impostare i filtri sui messaggi
			filtri possano essere applicati a livello di coda o a livello di consumer
			filtri a livello di coda
				da impostare nel file broker.xml
				in questo caso il filtro avviene prima che un messaggi arriva in una coda
				esempio:
				
					<addresses>
					   <address name="filter">
						  <queue name="filter">
							 <filter string="color='red'"/>
						  </queue>
					   </address>
					</addresses>
					
				in questo caso solo i messaggi con attributo color='red' sono inviati alla coda 
			filtri a livello di consumer
				quando creiamo il consumer passiamo il filtro da usare, es: MessageConsumer redConsumer = redSession.createConsumer(queue, "color='red'");
	gestione automatica di indirizzi e code
		con Artemis abbiamo la possibilita' di creare indirizzi e code automaticamente, con eliminazione automatica quando non sono piu' utilizzati
		questo toglie la necessita' di preconfigurare indirizzi e code prima che un client possa utilizzarli
		possiamo cambiare queste impostazioni nel broker.xml, es.
		
			<address-setting match="/news/politics/#">
			  <auto-create-addresses>true</auto-create-addresses>
			  <default-address-routing-type>MULTICAST</default-address-routing-type>
			</address-setting>
			
			in questo caso cambiamo le impostazioni per tutti indirizzi che iniziano con /news/politics/
			
	nome completo di una coda
		internamente il broker mappa una richiesta del client ad un indirizzo ad una coda ben precisa
		il broker si basa sulla propria configurazione e su in che modo il client ha istanziato la connessione
		MA il client puo' specificare la coda da usare in modo diretto
			in questo caso il client usa il nome completo della coda (fully qualified name)
			separando l'indirizzo dal nome con :: 
			per es. se abbiamo questa configurazione
			
			<addresses>
			   <address name="foo">
				  <anycast>
					 <queue name="q1" />
					 <queue name="q2" />
				  </anycast>
			   </address>
			</addresses>
			
			nel codice java scriviamo 
			
			String FQQN = "foo::q1";
			Queue q1 session.createQueue(FQQN);
			MessageConsumer consumer = session.createConsumer(q1);
			
	utilizzo di prefix per determinare il tipo di routing
		in teoria serve solo nel caso quando un indirizzo e' stato configurato con piu' tipi di routing
		possiamo impostare nel broker.xml nella sezione acceptors i prefissi da usare nella connessione al broker per passare il tipo di routing da usare 
	configurazione degli indirizzi avanzata
		spesso non e' richiesto preconfigurare le code, per esempio nel caso di un indirizzo point-to-point, quando un subscriber si connette all'indirizzo, la sua coda viene creata automaticamente
		il tipo di coda che viene creata dipende dalle proprieta' passate dal client (per es. durable, not-durable, not-shared, shared). 
		il Protocol Manager usa una naming convention per determinare quale coda appartiene al quale subscriber
		MA in certi casi possiamo avere esigenza di preconfigurare le code in modo che i client si connettono usando FQQN 
		una coda puo' essere configurata con durable e not-durable (flag purge-on-no-consumers)
		una coda non condivisa ha al max un consumer in ascolto (impostiamo max-consumers="1" a livello di coda)
		coda esclusiva per un consumer
			impostiamo il parametro exclusive="true" a livello di coda
			se abbiamo N consumer agganciato alla coda il broker ne sceglie uno e fa il redirect di tutti i msg verso quel consumer
			se il consumer scelto viene sganciato, ed esiste un'altro in ascolto sulla coda, il broker ne sceglie quello per il redirect di tutti i msg
	protocol managers
		ha una sua logica per determinare che tipo di code deve creare in base al protocollo
		NOTA: una coda viene eliminata se non ci sono i msg e consumer attivi solo se e' stata creata in modo automatico 
	e' possibile configurare gli indirizzi e code nella sezione <address-settings> di broker.xml
		e' facile configurare le impostazioni condivise con piu' indirizzi 
per poter connettersi ad Artemis usando il protocollo desiderato (o API JMS che sotto usano il protocollo nativo di Artemis) dobbiamo avere 
	la configurazione di acceptor dedicata, vedi sesione acceptors nel broker.xml
utilizzo di JMS
	Artemis usa la parte client di JNDI
		consente gestire oggetti configurabili come Broker, Topic e ConnectionFactory
		da usare solo se Artemis sta girando stand alone, fuori da un application server (es. Wildfly)
		esempio di come configurare la ConnectionFactory per una connessione remota
		
			java.naming.factory.initial=org.apache.activemq.artemis.jndi.ActiveMQInitialContextFactory
			connectionFactory.ConnectionFactory=tcp://myhost:5445
			
		nell'url impostato possiamo passare dei parametri di configurazione nella QueryString
			per es. se vogliamo usare una connessione SSL impostiamo tcp://remote-host:5445?ssl-enabled=true
			vedi doc http://localhost:8161/user-manual/configuring-transports.html per elenco di tutti parametri
			nella QueryString possiamo specificare anche il tipo di ConnectionFactory, di default e' CF (javax.jms.ConnectionFactory)
				valori possibili (CF, XA_CF, QUEUE_CF, QUEUE_XA_CF, TOPIC_CF, TOPIC_XA_CF)
	JNDI per la Destinazione
		Anche una Destinazione puo' essere recuperata usando JNDI
		una Destinazione puo' essere configurata usando proprieta' opportune per un contesto JNDI
		il nome di una proprieta' deve seguire il pattern queue.<jndi-binding> or topic.<jndi-binding>
		il valore della proprieta' deve contenere il nome della coda configurata su Artemis
		es. su Artemis abbiamo questa configurazione
		
			<address name="OrderQueue">
			   <queue name="OrderQueue"/>
			</address>
			
		le props di configurazione impostiamo in questo modo
		
			java.naming.factory.initial=org.apache.activemq.artemis.jndi.ActiveMQInitialContextFactory
			java.naming.provider.url=tcp://myhost:5445
			queue.queues/OrderQueue=OrderQueue
			
		in questo caso noi mappiamo indirizzo queues/OrderQueue alla coda OrderQueue
		a livello di codice java abbiamo:
		
			InitialContext ic = new InitialContext();
			//Now we'll look up the connection factory from which we can create
			//connections to myhost:5445:
			ConnectionFactory cf = (ConnectionFactory)ic.lookup("ConnectionFactory");
			//And look up the Queue:
			Queue orderQueue = (Queue)ic.lookup("queues/OrderQueue");
			//Next we create a JMS connection using the connection factory:
			Connection connection = cf.createConnection();
			//And we create a non transacted JMS Session, with AUTO\_ACKNOWLEDGE
			//acknowledge mode:
			Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
			//We create a MessageProducer that will send orders to the queue:
			MessageProducer producer = session.createProducer(orderQueue);
			//And we create a MessageConsumer which will consume orders from the
			//queue:
			MessageConsumer consumer = session.createConsumer(orderQueue);
			//We make sure we start the connection, or delivery won't occur on it:
			connection.start();
			//We create a simple TextMessage and send it:
			TextMessage message = session.createTextMessage("This is an order");
			producer.send(message);
			//And we consume the message:
			TextMessage receivedMessage = (TextMessage)consumer.receive();
			System.out.println("Got order: " + receivedMessage.getText());
			
	Possiamo usare JMS anche senza usare JNDI
		possiamo istanziare gli oggetti direttamente 
		vedi esempio nella doc.
	NOTA: lato nostra app abbiamo bisogno del jar che si trova sotto lib/client della distribuzione Artemis
Artemis supporta code con indirizzi wildcard, per es. coda associata all'indirizzo queue.news.# riceve i msg inviati agli indirizzi queue.news.europe e queue.news.usa
	in JMS questo e' noto come gerarchia di topic
Sintassi Wildcard
	. - il punto viene usato per separare le parole
	# - qualsiasi sequenza di 0 o piu' parole
	* - una parola singola
	usati di solito a livello di indirizzi o consumatori 
	e' possibile cambiare i separatori nella configurazione del broker, sezione <wildcard-addresses>
Espressioni di filtro
	vedi anche JMS Selectors (https://docs.oracle.com/javaee/7/api/javax/jms/Message.html)
	le espressioni di filtro possano essere usati in:
		- code predefinite nel broker.xml 
		- core bridges, solo i msg che metchano il filtro passano il ponte (vedi http://localhost:8161/user-manual/core-bridges.html)
		- divers, solo i mesaggi che metchano il filtro sono reindirizzati verso un'altro indirizzo 
		- a livello di consumer creati dal codice 
	filtri di Artemis sono un po' differenti dai JMS selectos
		filtri di Artemis aggiscono sui messaggi core
		filtri JMS aggiscono sui messagggi JMS
		ci sono identificatori predifiniti da usare con filtri Artemis
			AMQPriority - per referenziare la priorita' di un msg (0 min, 9 max)
			AMQExpiration
			AMQDurable - per fare il check se un msg e' durable (DURABLE) o no (NON_DURABLE)
			AMQTimestamp - timestamp di creazione msg
			AMQSize - dimensione del msg in byte
		tutti altri identificatori si presume che sono delle proprieta' di un msg
	NOTA: di default i valori sono stringhe e non sono convertiti in valori numerici (per es. per una espressione age > 18)
		per applicare la conversione nel filtro usare il prefisso convert_string_expressions: prima del nome di identificatore 
		es. convert_string_expressions:age > 18
	NOTA: per usare il trattino nei nomi di identificatori aggiungere il prefisso hyphenated_props: al nome (es. hyphenated_props:foo-bar = 0)
Persistenza
	Artemis supporta due opzioni di persistenza
		1. File Journal (ottimizzato per la messaggistica) - e' il default
		2. JDBC store (per connettersi al DB) NOTA: e' ancora in sviluppo, ma e' possibile usare le funzionalita' di journal (tranne la paginazione e msg grandi)
	File Journal
		append only journal
		e' composto da un set di file sul disco, creati a priori con dimensioni fisse.
		quando si riempie un file, viene eseguito il passaggio automatico al successivo
		tutte le scritture nel file sono in coda, si ottimizzano le prestazioni sul disco
		se un record di delete viene aggiunto al file, il sistema ha un meccanismo di garbage collection 
		alg di rimozioni spazi vuoti e compressione dati per utilizzare meno spazio su disco
		supporta il meccanismo di transazioni, sia trx locali che distribuiti (XA transaction)
		e' scritto in java, ma l'interfacciamento con file system e' astratto per permettere diverse implementazioni
			attualmente ci sono 2 implementazioni 
				1. Java NIO (utilizza Java NIO standard x interfacciarsi con file system), ottime prestazioni min Java6 richiesta
				2. Linux asynchronous IO, si interfaccia con la libreria linux AIO, Artemis vien notificato quando il dato e' stato scritto sul disco
					dovrebbe avere prestazioni piu' elevati rispetto NIO di Java
					prerequisito Linux kernel 2.6 o successivi
				3. Memory mapped, qui in mezzo (tra app e file system) abbiamo READ_WRITE memory mapping usando per pagine di SO cached (da approfondire)
		un server Artemis standard usa due istanze di File Journal
			- Bindings journal
				contiene i dati di binding (code deployate sul server con propri attributi)
				e' un NIO journal (non ha bisogno di un throughtput elevato)
				i file hanno il prefisso activemq-bindings
			- Message journal
				contiene i msg
				di default Artemis prova ad usare AIO ma se non e' linux o la libreria AIO non e' presente, viene fatto il fallback verso Java NIO
				i file hanno il prefisso activemq-data
				dimensione di default e' 10MB
				e' configurabile nel broker.xml, e' sonsigliato di dedicare un disco logico separato per i file di message journal
				possiamo impostare il tipo, uno di NIO, ASYNCIO or MAPPED
			i messaggi grandi (large) sono persistiti nei file dedicati, vedi http://localhost:8161/user-manual/large-messages.html
			i msg possano essere pagina, vedi http://localhost:8161/user-manual/paging.html
	Artemis puo' essere configurato a non persistere i msg proprio 
	Prima di scrivere sul disco Artemis gestisce una coda in memory ed esegue flush su disco quando questa coda è full o è passato un tempo prestabilito
	per impostare zero persistenza settare persistence-enabled a false nel broker.xml
Transport config
	a livello di server configuriamo acceptors per accettare le connessioni
	se il server a sua volta e' anche un client, configuriamo connectors (quando siamo in cluster o un server fa da ponte per un'altro)
	Artemis internamente usa Netty per le connessioni di rete
	server utilizza solo una porta per tutti protocolli
	vedi qui http://localhost:8161/user-manual/configuring-transports.html vari parametri configurabili
Configurazione del reload
	Artemis ricarica la configurazione se il broker.xml viene modificato (di default il check e' ogni 5 sec)
	I moduli ricaricati sono: Address Settings, Security Settings, Diverts, Addresses & queues
	le impostazioni di sicurezza (sezione security-settings) sono impostabili per indirizzo/i, se modificati, il sistema carica la nuova configurazione 
		senza necessita' di riavviare il broker
	le impostazioni per indirizzo/i sono configurabili nella sezione address-settings, , se modificati, il sistema carica la nuova configurazione 
	aggiornamento divers, on the fly sono deployati solo i nuovi divert
		quelli eliminati o modificati sono aggiornati solo dopo il restart del broker
	la sezione di indirizzi addresses, se aggiornata, viene aggiornata anche all'interno del broker in esecuzione, deployando o eliminando indirizzi (con code configurate)
	la sezione di code queues, se aggiornata, viene aggiornata anche all'interno del broker in esecuzione, deployando o eliminando code
Determinazione connessioni dead
	una connessione e' dead se un client e' creshato non liberando correttamente tutte le risorse necessarie
	alla chiusura di un client dobbiamo assicurarsi di liberare tutte le risrse usate (es. connessione), per esempio nel blocco finally
	se un client va in crash, e' possibile che lato server rimane appesa una sessione
		cmq Artemis gestisce anche la riconnesssione di client disconessi momentaneamente per i problemi della rete
		nella gestione di connessioni dead tiene conto anche di questo aspetto!
	una connessione ha la proprieta' TTL
		server tiene su una connessione in assenza dei dati per il tempo TTL
		ogni client periodicamente pinga il server 
		se il server non riceve i dati per il TTL, chiude la connessione e tutte le sessioni associate.
		TTL viene configurato nell'URI impostando il parametro connectionTTL, oppure server side nel broker.xml a livello globale
		broker di default ogni 2 sec verifica la presenza di connessioni da morte.
	cmq se noi in java non liberiamo opportunamente le risorse, queste risorse vengono determinate nel momento di garbage collection di broker,
		artemis scrive un warning nella console
		e cmq libera queste risorse al posto nostro 
		ma cmq dobbiamo fixare il nostro codice 
	anche lato client abbiamo in check se il server e' online, di default ogni 30sec
		se il server e' giu', lato client viene richiamato ExceptionListener (se usiamo JMS) o FailureListener 
Determinazione consumer lenti 
	consumer che ci mettono piu' tempo ad inviare ack al broker sono considerati lenti
	consumer lenti possano essere disconessi dal broker liberando delle risorse 
	di default il server non fa niente con consumer lenti, vedi le impostazioni a livello di indirizzo 
	alg di determinazione consumer lenti guarda il numero di ack inviati
		per ogni coda da monitorare viene usato un thread dal pool ScheduledThreadPoolExecutor 
Evitare isolamento della rete 
	quando due server in un cluster servono i msg
		puo' succedere quando una replica perde la connessione con il broker live 
	se abbiamo 3 e piu' server usiamo alg Quorum Voting
		il server di backup deve ricevere num voti configurati (min 2) prima di partire in modalita' live
	se abbiamo 2 server usiamo Network Pinger
	se il server live perde la connessione con il server di backup, deve attendere che il server di backup ritorna online di nuovo, prima di continuare la replica 
		qui puo' succedere il caso quando il server di backup torna online ed e' anche un server live 
		-> abbiamo due server live 
		in questo caso server live puo' iniziare il quorum a sua volta, se non riceve voti a sufficienza, va in shutdown, es. config. del master 
		
		<ha-policy>
		  <replication>
			<master>
			   <vote-on-replication-failure>true</vote-on-replication-failure>
			   <quorum-size>2</quorum-size>
			</master>
		  </replication>
		</ha-policy>
		
	possiamo configurare il ping della nostra rete (per es. pingare i nodi che fanno parte della topologia della nostra rete)
	se i nodi non sono raggiungibili il server si ferma finche' non ritornano online di nuovo 
Analisi dei problemi sul broker
	artemis misura i tempi in posti come
		- Queue delivery (add to the queue)
		- Journal storage
		- Paging operations
	se il tempo di risposta e' sotto un timeout configurato, il broker e' considerato instabile e viene arrestato oppure butta giu' la VM 
Gestione transazioni
	Artemis puo' essere configurato per determinare le transazioni vecchie e fare il loro rollback
	una transazione puo' essere appesa senza mai arrivare al commit se un client che la inizia viene disconesso e non torna piu' online
	vedi configurazione necessaria 
Gestione del flusso 
	serve per limitare il flusso di dati tra il client e server o tra i server 
	utile a non 'bombardare' un client/server con dei dati 
	impostazioni lato Consumer
		normalmente un client bufferizza i msg prima di consegnarli al consumer
			se un consumer non riesce a scodare in tempo i msg e la coda interna si riempie puo' capitare che client va in out of memory 
		Window based flow control 
			per ottimizzare le prestazioni e non accedere per ogni msg al server, lato client viene gestita una coda di messaggi 
			il parametro consumerWindowSize  viene usato per impostare le dimensioni del buffer 
				valori possibili
					-1: il buffer non ha un limite
					0: i msg non sono bufferizzati
					>0: dimensioni del buffer in bytes 
			di default e' 1MB
			da impostare questo parametro in base ai consumer, quanto siano veloci a scodare i msg 
				se i consumer sono veloci ha senso di non impostare le dimensioni del buffer (valore consumerWindowSize=-1)
				se i consumer sono lenti, magari non ha senso di avere il buffer locale, in modo che i msg si distribuiscono meglio tra i piu' consumer 
				vedi il caso descritto qui http://localhost:8161/user-manual/flow-control.html
		e' possibile configurare anche il rate con quale un consumer consuma i msg 
			impostare il parametro consumerMaxRate nell'URI 
	impostazioni lato Producer
		possiamo limitare i dati inviati dal producer verso il server per non sovracaricare il server 
		Window based flow control
			c'e' un alg basato sui crediti forniti da parte di server al producer 
			i crediti si basano sulle dimensioni di msg 
			se il producer ha crediti sufficienti, procede con l'invio di msg al server 
			il numero di crediti richiesti da producer e' noto con termine window size, ed e' controllato dal parametro producerWindowSize nell'URI 
			la dimensione della finestra e' il numero di byte che possano transitare prima di richiedere nuovi 
		usando client JMS server fornisce il numero di crediti richiesti dal producer 
			ma e' possibile impostare un numero massimo per ogni indirizzo
		quando la coda lato server e' full, i producer sono bloccati
			approccio e' simile a quello di una paginazione 
			quando i producer non sono bloccati e iniziano a scrivere i msg sullo storage
		i parametri da impostare sono max-size-bytes and address-full-policy
		il blocco viene applicato a tutte le code associate a quel indirizzo
			con il parametro max-size-bytes impostiamo max-size totale per tutte le code dell'indirizzo 
			di default abbiamo 10MB per indirizzo 
		possiamo limitare il rate (num. di msg inviati al secondo) anche lato producer
			parametro producerMaxRate  nell'URI 
Garanzie di invio e commit
	completamento di transazioni
		la richiesta di commit o rollback viene inviata al server, nel frattempo il client viene bloccato finche non riceve la risposta di commit/rollback eseguito 
		quando la richiesta viene ricevuta dal server, viene trasferito al journal e in base al parametro journal-sync-transactional, se true, la risposta viene inviata al client solo dopo aver persistito il msg di commit/rollback 
	invio di msg in modalita' non transazionale 
		quando inviamo i msg al server usando una sessione NON transazionale 
		un invio del msg puo' essere sincrono, cioe' il client aspetta una risposta dal server prima di continuare 
			ma questo aspetto e' configurabile nell'URI 
			blockOnDurableSend=true di default, tutti i msg durable inviati aspettano la risposta dal server 
			blockOnNonDurableSend=false di default, tutti i msg non durable inviati NON aspettano la risposta dal server 
		se blocchiamo il client abbiamo delle perdite nelle prestazioni 
			e' consigliato utilizzo di invio batch di msg all'interno di una transazione, in questo caso invio e' async e solo il commit/rollback e' sync 
			oppure usare asynchronous send acknowledgements feature (vedi sotto)
		parametro BlockOnAcknowledge lato consumer blocca il consumer finche non riceve la risposta dal server che ack e' stato consegnato 
		asynchronous send acknowledgements feature
			in questo caso l'invio di un msg in una sessione non transazionale non viene bloccato e riceve la risposta di avvenuta consegna del msg in un stream separato 
			dobbiamo implementare SendAcknowledgementHandler con metodo sendAcknowledged(ClientMessage message) e impostare handler lato ClientSession
			per usare questa modalita' dobbiamo essere certi che il parametro confirmationWindowSize e' stato impostato (per es. a 10MB)
Riconsegna dei msg + msg non consegnati 
	i msg possano fallire la consegna, con un rollback, in questo caso sono rimessi di nuovo nella coda per poter essere riconsegnati di nuovo 
	questo puo' causare un accumulo di msg non consegnati nella coda 
	ci sono due approcci per far fronte a questa situazione
		1. Riconsegna ritardata - posticipare la consegna di msg dando la possibilita' ad un client e ripristinare il proprio funzionamento
			vedi doc per impostazioni necessari 
			di default non c'e' nessun delay
		2. Indirizzo lettera morta (Dead Letter Address) - un indirizzo dedicato dove vanno tutti i msg non consegnati durante ultimi N tentativi 
			indirizzi del genere possano essere creati automaticamente, vedi doc
			una convenzione e' di usare il prefisso DLQ nel nome di indirizzo 
		entrambe le opzioni possano essere combinati per avere massima flessibilita' 
Scadenza dei msg
	per ogni msg possiamo impostare TTL 
		es. message.setExpiration(System.currentTimeMillis() + 5000);
			oppure a livello di producer producer.setTimeToLive(5000);
	artemis non consegnera' una msg con TTL scaduto al consumer 
	e' possibile configurare un indirizzo dedicato per msg scaduti (altrimenti il msg scaduro e' semplicemente eliminato dalla coda)
		i msg scaduti se consumati avranno seguenti props: _AMQ_ORIG_ADDRESS, _AMQ_ORIG_QUEUE, _AMQ_ACTUAL_EXPIRY (quando e' scaduto)
		questo indirizzo puo' essere creato automaticamente, per convenzione il prefisso da usare e' EXP.{indirizzo originale}
	possiamo impostare una scadenza di default a livello di indirizzo/i 
	di default nessuna scadenza impostata
	a livello di broker c'e' un thread dedicato che controlla la scadenza di msg 
		message-expiry-scan-period per configurare la frequenza, di default e' 30 sec 
Messaggi grandi 
	Artemis puo' essere configurato per salvare i msg come file se superano una certa dimensione 
	nella coda vengono salvati le info minime indispensabili con il riferimento al file contenente il msg completo 
	i file per questo tipo di msg sono depositati nella cartella impostata nel parametro large-messages-directory
	il parametro che determina se un msg e' considerato grande e' minLargeMessageSize
		di default e' 100KB
		NOTA: artemis usa la codifica 2byte per carattere, se il msg contiene i dati codificati ASCII (1byte per carattere) lato Artemis viene recodificato e il msg finale aumenta in dimensioni!
	i msg grandi possano essere compressi impostando il valore compressLargeMessages nell'URI 
		viene usato alg ZIP per comprimere il body del msg 
		lato server non ci sono differenze in gestione di msg con body compresso 
		la compressione e decompressione aviene client side 
	e' possibile fare lo streaming di msg large, vedi doc 
		utilizzabile anche usando JMS 
		vedi doc con esempi di codice 
Paginazione
	Artemis supporta code molto grandi che possano contenere mln di msg anche quando il server sta girando con memoria limitata
	in questo caso non e' possibile avere tutte le code in memoria 
	per questo Artemis pagina i msg fuori e in memoria al bisogno 
	la paginazione sul disco avviene quando la dimensione totale di msg per un indirizzo supera massima soglia consentita
	File di pagine
		sul file system i msg sono salvati per indirizzo
		ogni indirizzo ha la sua cartella dedicata dove i msg sono salvati in piu' file (page files)
		ogni file contiene max page-size-bytes
	oltre alla paginazione, quando viene superata la soglia, possiamo impostare i seguenti comportamenti x un indirizzo
		- DROP (msg viene droppato)
		- FAIL (producer riceve una eccezione)
		- BLOCK (producer viene bloccato)
	possiamo impostare la soglia anche sul disco max-disk-usage, se superata ogni msg successivo viene bloccato 
Schedulazione di messaggi
	un msg schedulato non viene consegnato finche non arriva un momento impostato 
	per schedulare il msg viene impostata una prop nel momento di invio 
	Message.HDR_SCHEDULED_DELIVERY_TIME
Coda Last-Value
	e' un tipo di coda dove un nuovo msg puo' sovrascrivere uno precedente 
	es. stock prices, siamo interessati solo all'ultimo prezzo 
	a livello di coda viene impostato il parametro last-value-key
	a livello di coda possiamo impostare il param non-destructive, se true, il msg letto non viene eliminato 
		per le code non Last-Value, se usiamo non-destructive, i msg non vengono mai eliminati e cresce il numero di msg continuamente
		per evitare il numero elevato di msg possiamo impostare il param expiry-delay
Coda Ring
	alg first-in, first-out (FIFO)
	e' una coda con dimensione fissa 
	msg aggiunti in coda, se massima dimensione raggiunta, ogni msg successivo va messo in fondo alla coda eliminando quello precedente 
Indirizzi retroattivi
	indirizzi senza code create possano accumulare i msg finche non si agganciano i subscriber creando automaticamente code dedicate 
	il numero di msg accomulabili e' configurabile, vedi doc
Code esclusive
	sono code che inviano i msg verso un consumer alla volta 
	e' utile per garantire l'ordine di scodamento 
Raggruppamento di msg 
	msg condividono lo stesso group id (JMSXGroupID for JMS)
	i msg dello stesso gruppo sono consumati dallo stesso consumer, anche se abbiamo piu' consumer agganciati alla coda 
		se il consumer corrente va offline, viene eletto un nuovo consumer 
	e' utile quando volgiamo processare i msg con stesse props in modo sequenziale
	per ogni msg che vogliamo raggruppare impostiamo la prop JMSXGroupID, oppure il parametro a livello della ConnectionFactory autogroup=true, o groupID={nome del gruppo}
	utilizzo di gruppi in un cluster e' complicato, vedi doc 
Prioritizzazione di consumer 
	consumer con piu' priorita' riceve i msg quando sono attivi 
	di default i consumer ricevono i msg in ordine Round Robin 
	priorita' di default e' 0, se tutti consumer hanno la stessa priorita' si applica Round Robin 
	invece se un consumer ha piu' priorita' di altri continua a ricevere i msg finche' ne ha i crediti, cioe' nella coda ci sono i msg che puo' scodare
	il parametro da impostare e' consmer-priority
Modalita' di acknowledgment
	JMS supporta tre modalita' 
		- AUTO_ACKNOWLEDGE
		- CLIENT_ACKNOWLEDGE
		- DUPS_OK_ACKNOWLEDGE
	Artemis ne aggiunge altri due
		- PRE_ACKNOWLEDGE (lato server il msg viene marcato come consegnato anche se non e' stato ancora completamento ricevuto dal client)
			qui c'e' rischio che perdiamo il msg quando abbiamo un fallimento del sistema e un ACK e' stato registrato lato server ma il msg non e' stato letto dal consumer 
			per usare questa modalita' configurare preAcknowledge=true nell'URL oppure usare ActiveMQJMSConstants.PRE_ACKNOWLEDGE nel momento di creazione della sessione 
		- INDIVIDUAL_ACKNOWLEDGE
			Individual ACK inherits all the semantics from Client Acknowledge, with the exception the message is individually acked.
Gestione API
	Artemis ha delle API di gestione molto esaustive 
	vedi doc http://localhost:8161/user-manual/management.html
Sicurezza 
	per disabilitare completamente la sicurezza possiamo impostare security-enabled=false nel broker.xml
	la config di sicurezza e' in cache con update periodici (di default ogni 10sec, param security-invalidation-interval), per motivi di prestazioni
	se populate-validated-user=true, ogni msg nel caso di JMS contiene la prop JMSXUserID valorizzata
	sicurezza basata sui ruoli x indirizzi
		sono stati previsti 8 permessi che possiamo associare ad un indirizzo o piu' indirizzi se usiamo attr match 
			createAddress
			deleteAddress
			createDurableQueue
			deleteDurableQueue
			createNonDurableQueue
			deleteNonDurableQueue
			send
			consume
			browse
			manage
		permessi sono per indirizzo/i e ci propagano alle code associate all'indirizzo 
		per ogni permesso possiamo impostare elenco di ruoli 
		utente appartiene ad un ruolo, e quindi eredita i permessi del ruolo 
	security manager gestisce il mapping tra utenti e ruoli 
		le credenziali sono letti dal file sul disco 
	usando SecuritySettingPlugin e' possibile configurare LDAP o un plugin di sicurezza custom
	e' possibile configurare Artemis per usare SSL 
	Artemis supporta due tipi di Security Manager 
		1. ActiveMQSecurityManager  (deprecato), la config letta dai file di props 
		2. ActiveMQJAASSecurityManager (default), supporto i moduli JAAS standard 
			JAAS - Java authentication and authorization service 
			la configurazione dipende dal modulo di login usato 
			la config di base e' nel bootstrap.xml (se impostiamo <jaas-security domain="PropertiesLogin"/>, la config viene letta dai file di props)
			il modulo di login che specifichiamo nella prop jaas-security viene letto dal file login.config
			il file login.config e' un file JAAS standard, vedi https://docs.oracle.com/javase/8/docs/technotes/guides/security/jgss/tutorials/LoginConfigFile.html
			i moduli di login di JAAS
				GuestLoginModule
					accesso al broker agli utenti gues (senza pwd, o con pwd errata)
					di solito e' accodato ad un'altro moduli di login (es. PropertiesLoginModule )
				PropertiesLoginModule
					fornisce uno store semplice per i dati di auth 
					sono dei file su disco 
				LDAPLoginModule
				CertificateLoginModule
				Krb5LoginModule
				ExternalCertificateLoginModule
	artemis lanciato con comando mask permette offuscare la pwd di un utente prima di inserirla nel file di prop (ENC (...)), vedi doc 
Artemis prevede un sistema a plugin
	implementiamo ActiveMQServerPlugin, metodi che ci interessano, vedi doc per dettagli
	possiamo configurare LoggingActiveMQServerPlugin esistente per loggare quello che ci interessa 
	possiamo configurare NotificationActiveMQServerPlugin
Limitare utilizzo di risorse
	possiamo impostare dei limiti sulle connessioni e code per un utente 
	vedi http://localhost:8161/user-manual/resource-limits.html
JMS Bridge
	puo' essere una app stand alone lanciata come una web app 
	praticamente preleva i msg dalla sorgente e li manda verso una destinazione (su un cluster differente, VM differente etc.)
	puo' prelevare i msg con filtri preimpostati (JMS selector)
	vedi doc http://localhost:8161/user-manual/jms-bridge.html
Client Reconnection and Session Reattachment
	un client puo' essere configurato per essere riconnesso di nuovo al server nel caso di perdita delle connessioni
	idem per sessioni, ricolleggamento avviene in modo trasparente per client 
		ttl configurabile lato server, tempo che server mantiene viva la sessione 
Redirect di msg 
	e' possibile configurare per un indirizzo di reindirizzare i msg verso un'altro indirizzo 
	trasparente per client 
	i msg possano cmq finire sia nella prima coda che nella seconda (configurabile)
Transformers
	possiamo implementare Transformer 
	consente agganciare una trasformazione ai msg, cambiando il body o le sue props
Duplicate Message Detection
	Artemis implementa un alg per eliminare i msg dupplicati
	tutto si basa sull'impostazione di un ID a livello di msg,
		org.apache.activemq.artemis.api.core.Message.HDR_DUPLICATE_DETECTION_ID
		va impostato come vogliamo ma deve garantire univocita' del msg 
Clustering
	da approfondire, http://localhost:8161/user-manual/clusters.html
Federazione
	replica di msg a livello di indirizzi e code
	anycasting e multicasting
	vedi varie topologie qui http://localhost:8161/user-manual/federation.html
HA
	repliche, backup etc..
	vedi doc http://localhost:8161/user-manual/ha.html
Gestione dei thread 
	ogni server artemis gestisce un pool di thread per uso generico e un scheduled thread pool per delle schedulazioni
	la dimensione del pool di default e' num. di core di SO x 3 (cambiabile impostando il param nioRemotingThreads)
	oltre ai due pool ci sono vari posti dove thread sono usati direttamente 
	Server Scheduled Thread Pool
		sono dei job eseguiti periodicamente o con un delay 
		la dimensione e' config impostando scheduled-thread-pool-max-size (default e' 5)
	General Purpose Server Thread Pool
		sono delle operazioni async server side 
		config impostando thread-pool-max-size (default e' 30)
	Expiry Reaper Thread
		e' un thread dedicato per fare il check sulla scadenza di msg 
	Asynchronous IO
		e' un pool dedicato per le operazioni async di IO
	Client-Side Thread Management
		qui abbiamo due pool
			1. "global" static scheduled thread pool (max 5 thread di default, param scheduledThreadPoolMaxSize nell'URI)
			2. "global" static general thread pool (param threadPoolMaxSize  nell'URI)
		NOTA: e' possibile impostare lato client di usare thread pool a livello di ClientSessionFactory  e non pool globali!
			param useGlobalPools nell'URL 
Logging
	usa JBoss Logging framework
	config in logging.properties
	di default logga sulla console e su file 
	per abilitare il logging lato client, aggiungiamo la dipendenze a jboss-logmanager
	
		<dependency>
		   <groupId>org.jboss.logmanager</groupId>
		   <artifactId>jboss-logmanager</artifactId>
		   <version>2.0.3.Final</version>
		</dependency>
		<dependency>
		   <groupId>org.apache.activemq</groupId>
		   <artifactId>activemq-core-client</artifactId>
		   <version>2.5.0</version>
		</dependency>
	
	cmq quando facciamo partire il nostro client dobbiamo impostare queste props
		1. JBoss Log Manager - es. -Djava.util.logging.manager=org.jboss.logmanager.LogManager
		2. location di logging.configuration, es. Dlogging.configuration=file:///home/user/projects/myProject/logging.properties
		vedi doc http://localhost:8161/user-manual/logging.html per esempio di config client side 
	possiamo config log di audit 
		di default e' disabilitato
	possiamo settare un nostro handler, deve estendere java.util.logging.Handler
		vedi http://localhost:8161/user-manual/logging.html per dettagli
Spring integration
	artemis puo' essere eseguito in modalita' embedded in un container DI 
		la modalita' embedded ha senso se la nostra app internamente si basa su un sistema di messaggistica 
			"Embedding Apache ActiveMQ Artemis can be done in very few easy steps. Instantiate the configuration object, instantiate the server, start it, and you have a Apache ActiveMQ Artemis running in your virtual machine. 
			It's as simple and easy as that."
	Apache ActiveMQ Artemis provides a simple bootstrap class, org.apache.activemq.artemis.integration.spring.SpringJmsBootstrap, for integration with Spring
		vedi esempio http://localhost:8161/user-manual/examples.html#spring-integration
Apache Tomcat Support
	Apache ActiveMQ Artemis provides support for configuring the client, in the tomcat resource context.xml of Tomcat container.
Intercepting Operations
	possiamo implementare interceptor che intercettano i pacchetti che arrivano al server o escono fuori 
	utile per logging 
	interceptor possano apportare delle modifiche ai pacchetti 
	vedi http://localhost:8161/user-manual/intercepting-operations.html
Maven plugin
	possiamo usare i plugin maven per gestire il ciclo di vita di server 
	il plugin di artemis e' usate per runnare server e fare delle operazioni di manutenzione
	noi lo possiamo usare per i test o deploy per esempio 
	goal previsti 
		create
		cli
		runClient
	vedi http://localhost:8161/user-manual/maven-plugin.html per dettagli 
Unit Testing 
	artemis fornisce il pacchetto artemis-junit per facilitare integrazione in JUnit
	vedi http://localhost:8161/user-manual/unit-testing.html per dettagli 
	
Altre risorse:
https://github.com/vromero/activemq-artemis-docker
https://github.com/apache/activemq-artemis


	
	
	
	