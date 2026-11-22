# Bill-Flex

## Description

**Bill-Flex** est une plateforme de facturation récurrente d'abonnements SaaS B2B, construite en architecture microservices.

Au renouvellement mensuel d'un abonnement, une facture est générée selon le plan souscrit, envoyée au client, encaissée, puis relancée automatiquement en cas d'impayé.

L'unité de traitement est la **Facture**, dont le cycle de vie est :

`BROUILLON` → `EMISE` → `PAYEE` / `EN_RETARD` → `ANNULEE`

Le projet comprend quatre services métier :

| Service | Responsabilité |
|---|---|
| `service-abonnements` | Catalogue de plans et abonnements clients |
| `service-facturation` | Factures et lignes de facturation (service principal) |
| `service-clients` | Comptes clients et paiements |
| `service-notifications` | Envoi et historique des notifications (facture émise, paiement reçu, relance) |

Ces services sont complétés par un serveur de découverte (Eureka), un serveur de configuration (Config Server) et une API Gateway.

---

## Diagramme de cas d'utilisation

```mermaid
flowchart LR
  admin["Administrateur"]
  client["Client"]
  planif["Planificateur"]
  pay["Passerelle de paiement"]
  mail["Serveur e-mail"]

  subgraph sys ["Système Bill-Flex"]
    direction TB
    uc1(["Gérer les plans"])
    uc2(["Gérer les clients"])
    uc3(["Suivre les impayés"])
    uc4(["Souscrire un abonnement"])
    uc5(["Résilier un abonnement"])
    uc6(["Consulter ses factures"])
    uc7(["Payer une facture"])
    uc8(["Générer les factures"])
    uc9(["Relancer les impayés"])
    uc10(["Notifier le client"])
  end

  admin --- uc1
  admin --- uc2
  admin --- uc3
  client --- uc4
  client --- uc5
  client --- uc6
  client --- uc7
  planif --- uc8
  planif --- uc9
  pay --- uc7
  mail --- uc10

  classDef actor fill:#e4ebf2,stroke:#1b3a5c,color:#1b3a5c;
  class admin,client,planif,pay,mail actor;
```

| Acteur | Cas d'utilisation |
|---|---|
| Administrateur | Gérer les plans, gérer les clients, suivre les impayés |
| Client | Souscrire un abonnement, résilier un abonnement, consulter ses factures, payer une facture |
| Planificateur (système) | Générer les factures à l'échéance, relancer les impayés |
| Passerelle de paiement | Intervient dans « Payer une facture » |
| Serveur e-mail | Intervient dans « Notifier le client » (facture émise, paiement reçu, relance) |

---

## Diagramme de classes UML global

Les flèches en pointillés représentent les dépendances entre services (références par identifiant, sans préjuger du protocole de communication).

```mermaid
classDiagram
  direction LR

  namespace service_abonnements {
    class Plan {
      -Long id
      -String nom
      -BigDecimal prixMensuel
      -int quotaUtilisateurs
      -boolean actif
    }
    class Subscription {
      -Long id
      -Long planId
      -Long clientId
      -LocalDate dateDebut
      -LocalDate dateFin
      -LocalDate prochaineEcheance
      -String statut
    }
  }

  namespace service_facturation {
    class Invoice {
      -Long id
      -Long subscriptionId
      -Long clientId
      -String periode
      -BigDecimal montantHT
      -BigDecimal montantTTC
      -LocalDate echeance
      -StatutFacture statut
    }
    class InvoiceLine {
      -Long id
      -Long invoiceId
      -String libelle
      -int quantite
      -BigDecimal prixUnitaire
    }
    class StatutFacture {
      <<enumeration>>
      BROUILLON
      EMISE
      PAYEE
      EN_RETARD
      ANNULEE
    }
  }

  namespace service_clients {
    class Client {
      -Long id
      -String nom
      -String email
      -String numeroTVA
      -String modePaiement
      -String statut
    }
    class Payment {
      -Long id
      -Long invoiceId
      -LocalDate date
      -BigDecimal montant
      -String moyen
      -String statut
    }
  }

  namespace service_notifications {
    class Notification {
      -Long id
      -Long clientId
      -Long invoiceId
      -TypeNotification type
      -String canal
      -String destinataire
      -String objet
      -String message
      -LocalDateTime dateEnvoi
      -StatutNotification statut
    }
    class TypeNotification {
      <<enumeration>>
      FACTURE_EMISE
      PAIEMENT_RECU
      RELANCE
    }
    class StatutNotification {
      <<enumeration>>
      EN_ATTENTE
      ENVOYEE
      ECHEC
    }
  }

  Plan "1" <-- "0..*" Subscription : repose sur
  Invoice "1" *-- "1..*" InvoiceLine : contient
  Invoice --> StatutFacture
  Notification --> TypeNotification
  Notification --> StatutNotification

  Notification ..> Invoice
  Notification ..> Client
  Invoice ..> Subscription
  Invoice ..> Client
  Subscription ..> Client
  Payment ..> Invoice
```

---

## Diagrammes de classes par microservice

Chaque service possède sa propre base de données : les références vers un autre service sont de simples identifiants (aucune clé étrangère, aucune relation JPA). L'existence de l'objet distant est vérifiée avant la création. Les classes marquées `<<distant>>` appartiennent à un autre service.

### service-abonnements

```mermaid
classDiagram
  direction LR
  class Plan {
    -Long id
    -String nom
    -BigDecimal prixMensuel
    -int quotaUtilisateurs
    -boolean actif
  }
  class Subscription {
    -Long id
    -Long planId
    -Long clientId
    -LocalDate dateDebut
    -LocalDate dateFin
    -LocalDate prochaineEcheance
    -String statut
  }
  class Client {
    <<distant>>
    service-clients
  }
  Plan "1" <-- "0..*" Subscription : repose sur
  Subscription ..> Client : clientId
```

### service-facturation

```mermaid
classDiagram
  direction LR
  class Invoice {
    -Long id
    -Long subscriptionId
    -Long clientId
    -String periode
    -BigDecimal montantHT
    -BigDecimal montantTTC
    -LocalDate echeance
    -StatutFacture statut
  }
  class InvoiceLine {
    -Long id
    -Long invoiceId
    -String libelle
    -int quantite
    -BigDecimal prixUnitaire
  }
  class StatutFacture {
    <<enumeration>>
    BROUILLON
    EMISE
    PAYEE
    EN_RETARD
    ANNULEE
  }
  class Subscription {
    <<distant>>
    service-abonnements
  }
  class Client {
    <<distant>>
    service-clients
  }
  Invoice "1" *-- "1..*" InvoiceLine : contient
  Invoice --> StatutFacture
  Invoice ..> Subscription : subscriptionId
  Invoice ..> Client : clientId
```

### service-clients

```mermaid
classDiagram
  direction LR
  class Client {
    -Long id
    -String nom
    -String email
    -String numeroTVA
    -String modePaiement
    -String statut
  }
  class Payment {
    -Long id
    -Long invoiceId
    -LocalDate date
    -BigDecimal montant
    -String moyen
    -String statut
  }
  class Invoice {
    <<distant>>
    service-facturation
  }
  Payment ..> Invoice : invoiceId
```

### service-notifications

```mermaid
classDiagram
  direction LR
  class Notification {
    -Long id
    -Long clientId
    -Long invoiceId
    -TypeNotification type
    -String canal
    -String destinataire
    -String objet
    -String message
    -LocalDateTime dateEnvoi
    -StatutNotification statut
  }
  class TypeNotification {
    <<enumeration>>
    FACTURE_EMISE
    PAIEMENT_RECU
    RELANCE
  }
  class StatutNotification {
    <<enumeration>>
    EN_ATTENTE
    ENVOYEE
    ECHEC
  }
  class Invoice {
    <<distant>>
    service-facturation
  }
  class Client {
    <<distant>>
    service-clients
  }
  Notification --> TypeNotification
  Notification --> StatutNotification
  Notification ..> Invoice : invoiceId
  Notification ..> Client : clientId
```

---

## Architecture

Le projet est composé de sept applications :

| Application | Rôle | Port |
|---|---|---|
| `api-gateway` | Point d'entrée unique, routage des requêtes | **8080** |
| `eureka-server` | Serveur de découverte de services | **8761** |
| `config-server` | Configuration externalisée des services | **8888** |
| `service-abonnements` | Plans et abonnements | **8081** |
| `service-facturation` | Factures et lignes de facturation | **8082** |
| `service-clients` | Clients et paiements | **8083** |
| `service-notifications` | Notifications (e-mails) | **8085** |

```mermaid
flowchart TB
  client(["Client / Front Angular"])

  gateway["api-gateway<br/>:8080"]
  eureka["eureka-server<br/>:8761"]
  config["config-server<br/>:8888"]

  subgraph metier ["Services métier"]
    abo["service-abonnements<br/>:8081"]
    fact["service-facturation<br/>:8082"]
    cli["service-clients<br/>:8083"]
    notif["service-notifications<br/>:8085"]
  end

  client --> gateway
  gateway --> abo
  gateway --> fact
  gateway --> cli
  gateway --> notif

  fact -.-> abo
  fact -.-> cli
  cli -.-> fact
  abo -.-> fact
  abo -.-> cli
  fact -.-> notif
  notif -.-> cli

  gateway -. enregistrement .-> eureka
  metier -. enregistrement .-> eureka
  config -. configuration .-> gateway
  config -. configuration .-> metier
```

---

## Équipe

Brahim · Selma · Aymane · Hadil