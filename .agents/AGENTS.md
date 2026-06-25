# Règles de Développement du Projet (Project-Scoped Rules)

Ce document consigne les directives et contraintes spécifiques à ce projet pour guider les futurs développements.

## Gestion de la Base de Données (Room SQLite)

> [!IMPORTANT]
> **Règle absolue pour tout changement de schéma de base de données :**
> 1. **Désactiver la suppression automatique** : Ne jamais utiliser ou réactiver `.fallbackToDestructiveMigration()` en production.
> 2. **Rédiger des scripts de migration explicites** : Chaque modification de table (ajout de colonne, modification de type, indexation) doit faire l'objet d'un objet `Migration(X, Y)` défini dans `DatabaseModule.kt`.
> 3. **Écrire des tests unitaires de migration** : Avant de valider et de pusher un changement de base de données sur GitHub, implémenter un test d'intégration de migration de schéma (utilisant la bibliothèque de test `androidx.room:room-testing`) pour valider la transition sans perte de données.
