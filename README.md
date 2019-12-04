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
		- récupère un json des locs SENT=0 et dont le fixtime est pas plus vieux que age_maximum_des_fixtimes (getJsonOfLocs dans BaseDeDonnees.java)
		- si le json est pas vide: lance une asyncTask requete POST http envoi du JSON, et à la fin (onPostExecute) 
			- passe à SENT=1 les rows du json reçu par l'async task dans la table loc de la BDD
			- écrit dans la table net de la BDD le n° de l'async, les times de début et de fin de l'async, le nombre de locs, la latlng du fixtime le plus grand dans le json (donc la position la plus fraiche au moment
			du début de l'envoi)
			- update une textview de l'UI en appelant une méthode static de l'activité principale (oui c'est possible, voir les commentaires au dessus de cette methode "updateSent")
			

# # ToDo list

* Récolte des données network:
	- Traquer les failures: n° identifiant unique pour voir si je les ai toutes (certaines pas finies?), 
	- Données GSM: dans BaseDeDonnees.java -> entrer dans la bdd, si tu penses que c'est important.
* Niveau Batterie / altitude accuracy (?)
	- ajouter -> alt acc marche, mais pas denvoi pour linstant, pas géré le front
	- faire un howto pour chaque nouvel ajout ultérieur (car ça modifie aussi serveur et front...)	
* Maintenant que je n'envoie que des locs récentes, mark as unsent à 2 quand bouton stop n'a plus de sens. Enlever pour simplifier.
* Détection pause/repart: quand je m'arrête: j'ai pas envie que ma bdd soit bloatée de points les uns à côté des autres... Quand points super proches, il
	ne faudrait que le dernier, ou alors noter de ne pas envoyer les autres? Il faudrait pouvoir utiliser une librairie géographique. Et le premier endpoint
	serait de détecter une pause. Quoi faire quand on la détecte: on verra après (modif de la fréquence des requetes GPS? Comment gérer les envois?).

* UI (https://www.androidauthority.com/android-ui-views-1018249/)
	- dialog de confirmation pour stop
* Fixtime: location.getTime() -> provenance: system ou GNSS??
	- essayé: fausse heure sur tel + empêché de récup heure sur réseau -> les fixtimes restent à la bonne heure! -> noter dans le code et enlever ça d'ici
* Identifiant unique pour du multi-utilisateur
	- a l'installation la première fois?
	- commencer par modèle pour ne pas se prendre la tête?
* Un menu pour des tâches annexes
* Données récoltées en bluetooth (esp32 / capteur)
* UI: Permettre choix fréquence requestLocations / fréquence POST
* Permissions check au startup: pour utilisation "commerciale"
* icones
	- foreground service
	- appli
* Nettoyage de la bdd

# # Littérature

* Grosse discussion C2C post simon gauthier https://forum.camptocamp.org/t/geolocalisation-smartphone/249306/67
