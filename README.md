# Bill-Flex

## Description

**Bill-Flex** est une plateforme de facturation récurrente d'abonnements SaaS B2B, construite en architecture microservices.

Au renouvellement mensuel d'un abonnement, une facture est générée selon le plan souscrit, envoyée au client, encaissée, puis relancée automatiquement en cas d'impayé.

L'unité de traitement est la **Facture**, dont le cycle de vie est :

`BROUILLON` → `EMISE` → `PAYEE` / `EN_RETARD` → `ANNULEE`

Le projet comprend trois services métier :

| Service | Responsabilité |
|---|---|
| `service-abonnements` | Catalogue de plans et abonnements clients |
| `service-facturation` | Factures et lignes de facturation (service principal) |
| `service-clients` | Comptes clients et paiements |

Ces services sont complétés par un serveur de découverte (Eureka) et une API Gateway.

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

  Plan "1" <-- "0..*" Subscription : repose sur
  Invoice "1" *-- "1..*" InvoiceLine : contient
  Invoice --> StatutFacture

  Invoice ..> Subscription
  Invoice ..> Client
  Subscription ..> Client
  Payment ..> Invoice
```

---

## Architecture

Le projet est composé de cinq applications :

| Application | Rôle | Port |
|---|---|---|
| `api-gateway` | Point d'entrée unique, routage des requêtes | **8080** |
| `eureka-server` | Serveur de découverte de services | **8761** |
| `service-abonnements` | Plans et abonnements | **8081** |
| `service-facturation` | Factures et lignes de facturation | **8082** |
| `service-clients` | Clients et paiements | **8083** |

```mermaid
flowchart TB
  client(["Client / Front Angular"])

  gateway["api-gateway<br/>:8080"]
  eureka["eureka-server<br/>:8761"]

  subgraph metier ["Services métier"]
    abo["service-abonnements<br/>:8081"]
    fact["service-facturation<br/>:8082"]
    cli["service-clients<br/>:8083"]
  end

  client --> gateway
  gateway --> abo
  gateway --> fact
  gateway --> cli

  fact -.-> abo
  fact -.-> cli
  cli -.-> fact
  abo -.-> fact

  gateway -. enregistrement .-> eureka
  abo -. enregistrement .-> eureka
  fact -. enregistrement .-> eureka
  cli -. enregistrement .-> eureka
```

---

## Équipe

Brahim · Selma · Aymane · Hadil
