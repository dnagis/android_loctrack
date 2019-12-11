# LocTrack v2 (2019)

Je mets les sources de 2017 dans backup_2017 en tarball

La partie Http sur Alrm vient de git@github.com:dnagis/android_url_alrm.git

# # Fonctionnement:

* LocTrack_Activity.java:
	- boolean sessionRunning
	- bouton start() -> si sessionRunning = false (par défaut boolean sont false) 
		- Dialog (attention écraser données) -> OK / cancel
		si OK:
		launch_le_bousin() qui:
		- delete all dans les deux tables de la bdd
		- Crée la base de données pour les locs maBDD = new BaseDeDonnees(this); -> onCreate() de cette classe
		- Crée si n'existe pas un locationManager -> dans sa callback onLocationChanged() écrit dans la base de données la loc (BaseDeDonnees.java)
		- Crée si n'existe pas un AlarmManager pour déclencher LocTrackAlarm régulièrement
		- Lance un foregroundservice (startForegroundService) dont le seul et unique but et de maintenir l'importance
	- le bouton stop arrête {alrm,location}Manager et le foreground service, et passe les non envoyés à SENT=2 (pour pas qu'ils soient
	pris en compte dans une session ultérieure, mais en gardant l'info qu'ils ont pas été envoyés -SENT à 2 et pas à 1-).
	- ne pas passer dans onCreate() à chaque rotation d'écran: android:configChanges="orientation|screenLayout|screenSize" dans le manifest
	
* LocTrackAlarm.java: 
	- à chaque déclenchement d'Alarm dans le onStartCommand() 
		- récupère un json des locs SENT=0 et dont le fixtime est pas plus vieux que age_maximum_des_fixtimes (getJsonOfLocs dans BaseDeDonnees.java), parce que j'ai déjà
			eu un soucis lors d'une mini ballade avec bloquage, et des json qui grossissaient à chaque alarm.
		- si le json est pas vide: lance une asyncTask requete POST http envoi du JSON, et à la fin (onPostExecute) 
			- passe à SENT=1 les rows du json reçu par l'async task dans la table loc de la BDD
			- écrit dans la table net de la BDD le n° de l'async, les times de début et de fin de l'async, le nombre de locs, la latlng du fixtime le plus grand dans le json (donc la position la plus fraiche au moment
			du début de l'envoi)
			- update une textview de l'UI en appelant une méthode static de l'activité principale (oui c'est possible, voir les commentaires au dessus de cette methode "updateSent")
			

# # ToDo list

* Niveau Batterie: log bdd et upload
	- en profiter pour faire un howto pour chaque nouvel ajout ultérieur (car ça modifie aussi serveur et front...)	
* Text input pour du tooltip affiché
* Maintenant que je n'envoie que des locs récentes, mark as unsent à 2 quand bouton stop n'a plus de sens. Enlever pour simplifier.
* Détection pause/repart: quand je m'arrête: j'ai pas envie que ma bdd soit bloatée de points les uns à côté des autres... Quand points super proches, il
	ne faudrait que le dernier, ou alors noter de ne pas envoyer les autres? Il faudrait pouvoir utiliser une librairie géographique. Et le premier endpoint
	serait de détecter une pause. Quoi faire quand on la détecte: on verra après (modif de la fréquence des requetes GPS? Comment gérer les envois?).
* UI / menu (https://www.androidauthority.com/android-ui-views-1018249/)
	- dialog de confirmation pour stop
	- Permettre choix fréquence requestLocations / fréquence POST
* Fixtime: location.getTime() -> provenance: system ou GNSS??
	- essayé: fausse heure sur tel + empêché de récup heure sur réseau -> les fixtimes restent à la bonne heure! -> noter dans le code et enlever ça d'ici
* Identifiant unique pour du multi-utilisateur
	- a l'installation la première fois?
	- commencer par modèle pour ne pas se prendre la tête?
* Données récoltées en bluetooth (esp32 / capteur)
* Permissions check au startup: pour utilisation "commerciale"
* icones
	- foreground service
	- appli


# # Littérature

* Grosse discussion C2C post simon gauthier https://forum.camptocamp.org/t/geolocalisation-smartphone/249306/67
