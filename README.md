# Casse-Brique v3 — Java 21, réseau, nouveaux décors

## Lancer / compiler
- **Dev (Eclipse / IntelliJ / ligne de commande)** : JDK 21 + Maven. `mvn javafx:run` lance le jeu.
  Eclipse : *Import > Existing Maven Projects*. Sans Maven : télécharger le SDK JavaFX 21 (gluonhq.com/products/javafx)
  et ajouter aux VM arguments : `--module-path "C:\javafx-sdk-21\lib" --add-modules javafx.controls,javafx.media`
  (classe principale `cassebrique.Main`).
- **Tes fichiers .wav existants** : les mettre dans `src/main/resources/cassebrique/sounds/` (3 nouveaux sons fournis :
  `rainbow.wav`, `swap.wav`, `lava.wav`, simples à remplacer).

## Envoyer le jeu à quelqu'un (sans qu'il installe Java)
Sur Windows, avec JDK 21 + Maven : double-clic sur `package-windows.bat`.
Il produit `dist\CasseBrique\` (Java + JavaFX inclus, ~150 Mo) : **zipper ce dossier, l'envoyer, la personne
décompresse et lance `CasseBrique.exe`**. Aucune installation. (Installateur classique possible avec WiX, voir le .bat.)

## Jouer en réseau
Menu > onglet **2 JOUEURS** > **JOUER EN RÉSEAU**.
- Joueur 1 : *Héberger* (il est BLANC) ; l'écran affiche son adresse IP et le port (TCP 5757).
- Joueur 2 : *Rejoindre* (il est NOIR) ; tape l'adresse (Ctrl+V pour coller).
- Une fois connectés, l'hôte choisit le niveau 2 joueurs ; chacun joue à la souris.
- Même réseau local : rien d'autre. Par Internet : redirection du port TCP 5757 sur la box de l'hôte, ou VPN gratuit
  (Tailscale, ZeroTier, Radmin VPN). Les deux ordinateurs doivent avoir la même version du jeu.
- L'hôte fait tourner toute la partie ; l'invité reçoit ~60 images/s (sa propre raquette reste fluide, sans retard).

## Niveaux : nouveautés
- `soloFull("nom", ...)` : briques collées au plafond et aux deux murs (largeur adaptée au nombre de colonnes) ;
  `.` pour les espaces. `solo(...)` garde les marges habituelles (jusqu'à 15 colonnes).
- Nouveaux caractères : `y` malus jaune (tête de mort), `a` arc-en-ciel, `x` échange, `l` balle de lave, `v` ivresse.
