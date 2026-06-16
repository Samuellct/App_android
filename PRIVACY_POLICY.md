# Politique de Confidentialité - Work Log

Dernière mise à jour : 16 juin 2026

L'application **Work Log** (ci-après "l'Application") est conçue et développée comme une application entièrement autonome, fonctionnant en mode local ("offline-first"). Nous accordons une importance primordiale au respect de votre vie privée et à la protection de vos données.

---

## 1. Collecte et Traitement des Données
* **Aucune transmission réseau** : L'Application ne collecte, ne transmet et ne partage aucune de vos données personnelles avec des serveurs externes.
* **Données de suivi local** : Toutes les données que vous saisissez (missions, taux horaires, heures travaillées, primes, commentaires) sont stockées de façon strictement locale et sécurisée dans la base de données interne (SQLite Room) de votre propre appareil Android.
* **Aucun compte requis** : L'utilisation de l'Application ne nécessite aucune inscription, création de compte ou connexion internet.

---

## 2. Sauvegarde des Données (Google Auto Backup)
L'Application utilise le service de sauvegarde automatique standard d'Android (**Google Auto Backup**). Si cette option est activée sur votre téléphone Android :
* Vos données locales (base de données de suivi et préférences) sont chiffrées et stockées dans votre espace personnel sécurisé Google Drive associé à votre compte Android.
* Ce service est géré intégralement par le système d'exploitation Android et Google, et permet de restaurer automatiquement vos heures en cas de réinstallation de l'Application ou de changement de téléphone. Aucun tiers (y compris le développeur de l'Application) n'a accès à ces données.

---

## 3. Autorisations requises par l'Application
L'Application requiert les permissions suivantes pour son bon fonctionnement :
* **Notifications (Android 13+)** : Utilisée uniquement pour vous envoyer une alerte de rappel quotidienne de saisie d'heures (si vous l'avez configurée dans les paramètres).
* **Vibreur (Retour haptique)** : Utilisée pour confirmer la bonne validation de la saisie d'heures par une vibration physique légère.

---

## 4. Exportations de Données
Vous êtes le seul propriétaire de vos données. L'Application propose des fonctions d'exportation de données (fichiers CSV ou relevés d'heures PDF). Ces exports sont générés localement sur votre téléphone et ne sont partagés avec des tiers (comme votre agence d'intérim) que si vous effectuez vous-même explicitement l'action de partage via le menu de partage Android.

---

## 5. Contact
Pour toute question concernant cette Politique de Confidentialité, vous pouvez consulter le code source de l'application ou ouvrir un ticket sur le dépôt officiel :
* Dépôt GitHub : [https://github.com/Samuellct/App_android](https://github.com/Samuellct/App_android)
