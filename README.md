# LocTrack v2 (2019)

Je mets les sources de 2017 dans backup_2017 en tarball

La partie Http sur Alrm vient de git@github.com:dnagis/android_url_alrm.git

# # Fonctionnement:

* LocTrack_Activity.java:
	- boolean sessionRunning
	- bouton start() -> si sessionRunning = false (par défaut boolean sont false) -> launch_le_bousin qui:
		- Crée la base de données pour les locs maBDD = new BaseDeDonnees(this); -> onCreate() de cette classe
		- Crée si n'existe pas un locationManager -> dans sa callback onLocationChanged() écrit dans la base de données la loc (BaseDeDonnees.java)
		- Crée si n'existe pas un AlarmManager pour déclencher LocTrackAlarm régulièrement
		- Lance un foregroundservice (startForegroundService) dont le seul et unique but et de maintenir l'importance
	- le bouton stop arrête {alrm,location}Manager et le foreground service, et passe les non envoyés à SENT=2 (pour pas qu'ils soient
	pris en compte dans une session ultérieure, mais en gardant l'info qu'ils ont pas été envoyés -SENT à 2 et pas à 1-).
	
* LocTrackAlarm.java: 
	- à chaque déclenchement d'Alarm dans le onStartCommand() 
		- récupère un json des locs SENT=0 (getJsonOfLocs)
		- lance une asyncTask requete POST http envoi du JSON, et à la fin (onPostExecute) passe à SENT=1 dans la BDD

# # ToDo list

* Arrêt session
	- si plusieurs sessions successives: les points non envoyés de la dernière session faussent la session d'après (rxtime)
	vérifier fonctionnement essais reallife
	- dialog de confirmation (appui bouton par erreur???)
* UI: 
	- time dernier envoi successful, dernière latlng -> dynamique si possible (comme dans tv_vvnx) https://www.androidauthority.com/android-ui-views-1018249/
	- dialog de confirmation
* Démarrage session: 	
	- si l'heure du tel est fausse+++ le fixtime envoyé est foireux: dans données GNSS j'ai qqchose? sinon check réseau via une route
	sur mon serveur...
* Les points plus anciens que... ne doivent pas être prioritaires pour l'envoi: c'est le dernier qui prime	
* Identifiant unique pour du multi-utilisateur
	- a l'installation la première fois?
	- commencer par modèle pour ne pas se prendre la tête?
* Niveau Batterie et Signal GSM
	- ajouter
	- faire un howto pour chaque nouvel ajout ultérieur (car ça modifie aussi serveur et front...)
* Un menu pour des taches alternes
* Données récoltées en bluetooth (esp32 / capteur)
* Avoir le délai de la requête POST: possible dans l'AsyncTask? Si oui une bdd spéciale pour ça? Utilité?
* Choix fréquence requestLocations / fréquence POST
* Permissions check au startup: pour utilisation "commerciale"
* icones
	- foreground service
	- appli
* Nettoyage de la bdd
