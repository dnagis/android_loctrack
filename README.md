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

# # ToDo

* Essayer sur production build
* Boutons pour démarrage / arrêt
* Identifiant téléphone (modèle suffit) à passer dans le JSON
* Niveau Batterie et Signal GSM
* Avoir le délai de la requête POST: possible dans l'AsyncTask? Si oui une bdd spéciale pour ça?
* Choix fréquence GPS / fréquence POST
