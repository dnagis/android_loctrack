# LocTrack v2 (2019)

Je mets les sources de 2017 dans backup_2017 en tarball

La partie Http sur Alrm vient de git@github.com:dnagis/android_url_alrm.git

# # Fonctionnement:

* LocTrack_Activity.java:
	- onCreate() -> une seule fois la première fois (if(savedInstanceState == null)) -> launch_le_bousin qui:
		- crée la base de données pour les locs maBDD = new BaseDeDonnees(this); -> onCreate() de cette classe
		- Lance un locationManager -> dans sa callback onLocationChanged() écrit dans la base de données la loc (BaseDeDonnees.java)
		- Lance un AlarmManager pour déclencher LocTrackAlarm régulièrement
		- Lance un foregroundservice (startForegroundService) dont le seul et unique but et de maintenir l'importance
	- le bouton stop arrête le foreground service
		
	
* LocTrackAlarm.java: 
	- à chaque déclenchement d'Alarm dans le onStartCommand() 
		- récupère un json des locs SENT=0 (getJsonOfLocs)
		- lance une asyncTask requete POST http envoi du JSON, et à la fin (onPostExecute) passe à SENT=1 dans la BDD

# # ToDo list

* Démarrage session / arrêt
	- arrêt: un dernier essai d'envoi de la totalité des rows, si échec passer sent à 9 (pas 0)
	- boutons start/stop avec confirmation pour chacun
* Démarrage : check réseau + time sur réseau
	- si l'heure du tel est fausse+++ le fixtime envoyé est foireux: dans données GNSS j'ai qqchose?
* Affichage main screen: time dernier envoi successful, dernière latlng
* Les points plus anciens que... ne doivent pas être prioritaires pour l'envoi: c'est le dernier qui prime	
* Identifiant unique pour du multiutilisateur
	- a l'installation la première fois?
	- commencer par modèle pour ne pas se prendre la tête?
* Niveau Batterie et Signal GSM
	- ajouter
	- faire un howto pour chaque nouvel ajout ultérieur
* Données récoltées en bluetooth (esp32 / capteur)
* Avoir le délai de la requête POST: possible dans l'AsyncTask? Si oui une bdd spéciale pour ça?
* Choix fréquence GPS / fréquence POST
* Permissions check au startup: pour utilisation "commerciale"
* icones
	- foreground service
	- appli
