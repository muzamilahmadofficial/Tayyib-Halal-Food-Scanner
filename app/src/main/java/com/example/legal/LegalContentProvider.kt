package com.example.legal

enum class LegalDocumentType {
    PRIVACY_POLICY,
    TERMS_OF_USE,
    RELIGIOUS_DISCLAIMER,
    HALAL_METHODOLOGY,
    DATA_ATTRIBUTION,
    OPEN_SOURCE_LICENSES
}

data class LegalDocument(
    val titleEn: String,
    val titleFr: String,
    val sectionsEn: List<Pair<String, String>>,
    val sectionsFr: List<Pair<String, String>>
)

object LegalContentProvider {

    fun getDocument(type: LegalDocumentType): LegalDocument = when (type) {
        LegalDocumentType.RELIGIOUS_DISCLAIMER -> LegalDocument(
            titleEn = "Religious Disclaimer",
            titleFr = "Avertissement religieux",
            sectionsEn = listOf(
                "Informational Purpose Only" to
                    "Tayyib provides informational assessments based on available product data, ingredient lists, labels, community evaluations and other referenced sources.",
                "Independent Application" to
                    "Tayyib is an independent application and is not a Halal certification authority or religious institution.",
                "Data Limitations" to
                    "Product information may be incomplete, outdated or incorrect. A database assessment does not necessarily constitute independent certification.",
                "Scholarly Differences" to
                    "Halal interpretations can differ between scholars, certification organizations and Islamic schools of thought.",
                "Personal Verification" to
                    "Users should check product packaging and consult trusted certification bodies or qualified religious authorities when uncertain.\n\nTayyib does not guarantee that any product is Halal, and its assessments should not replace personal verification or religious guidance."
            ),
            sectionsFr = listOf(
                "Caractère strictement informatif" to
                    "Tayyib fournit des évaluations à titre informatif basées sur les données produit disponibles, les listes d'ingrédients, les mentions sur l'emballage, les évaluations communautaires et d'autres sources référencées.",
                "Application indépendante" to
                    "Tayyib est une application indépendante et n'est ni un organisme de certification Halal ni une institution religieuse.",
                "Limites des données" to
                    "Les informations sur les produits peuvent être incomplètes, obsolètes ou inexactes. Une évaluation issue d'une base de données ne constitue pas une certification indépendante.",
                "Divergences savantes" to
                    "Les interprétations relatives au Halal peuvent varier selon les savants, les organismes certificateurs et les écoles juridiques islamiques.",
                "Vérification personnelle" to
                    "Les utilisateurs doivent vérifier l'emballage du produit et consulter des organismes de certification reconnus ou des autorités religieuses qualifiées en cas de doute.\n\nTayyib ne garantit pas qu'un produit soit Halal et ses évaluations ne remplacent pas la vérification personnelle ni l'avis religieux qualifié."
            )
        )

        LegalDocumentType.PRIVACY_POLICY -> LegalDocument(
            titleEn = "Privacy Policy",
            titleFr = "Politique de confidentialité",
            sectionsEn = listOf(
                "Zero Account & Local-First Privacy" to
                    "Tayyib: Halal Food Scanner is developed independently by Muzamil Ahmad. The application works completely without creating an account, signing in, or providing your name, email address, or phone number.",
                "Data Stored on Your Device" to
                    "Your scan history, saved favourites, cached product lookups, and app preferences (language, theme, sound, vibration) are stored locally on your device using Android Room and DataStore. You can clear scan history or product cache at any time in Settings.",
                "Camera & Photo Picker" to
                    "Camera access is used solely on-device via CameraX and Google ML Kit (Barcode Scanning and Lens Text Recognition) to read product barcodes and ingredient labels in real time. Photo selection uses Android's zero-permission Photo Picker. Tayyib does not record, store, or transmit your photos or camera frames.",
                "Network Requests" to
                    "When you scan a barcode or search for a product online, Tayyib sends HTTPS requests containing only the barcode or search query to Halal Open Food Facts (halalopenfoodfacts.org), with automatic live fallback to Open Food Facts (world.openfoodfacts.org) if the primary service is unavailable. No advertising IDs, analytics trackers, or user profiles are transmitted.",
                "No Ads or Third-Party Tracking" to
                    "Tayyib contains no advertising SDKs, no behavioural analytics trackers, and does not sell or share personal data.",
                "Contact" to
                    "For privacy inquiries, contact the independent developer Muzamil Ahmad at Muzamilofficially@gmail.com."
            ),
            sectionsFr = listOf(
                "Sans compte et confidentialité locale" to
                    "Tayyib : Scanner Alimentaire Halal est développé de manière indépendante par Muzamil Ahmad. L'application fonctionne entièrement sans création de compte, sans connexion et sans collecte de nom, d'e-mail ou de numéro de téléphone.",
                "Données enregistrées sur votre appareil" to
                    "Votre historique de scans, vos favoris, le cache des produits consultés et vos préférences (langue, thème, son, vibration) sont conservés exclusivement sur votre appareil via Room et DataStore. Vous pouvez les effacer à tout moment dans les Paramètres.",
                "Caméra et sélecteur de photos" to
                    "L'accès à la caméra sert uniquement sur l'appareil via CameraX et Google ML Kit (lecture de codes-barres et reconnaissance de texte Lens) pour lire les codes-barres et étiquettes d'ingrédients. La sélection d'image utilise le sélecteur Android sans autorisation de stockage. Aucune photo ni vidéo n'est enregistrée ou transmise.",
                "Requêtes réseau" to
                    "Lors d'un scan ou d'une recherche en ligne, Tayyib transmet uniquement le code-barres ou les termes de recherche en HTTPS à Halal Open Food Facts (halalopenfoodfacts.org), avec bascule automatique de secours vers Open Food Facts (world.openfoodfacts.org) en cas d'indisponibilité. Aucun identifiant publicitaire ni profil utilisateur n'est transmis.",
                "Aucune publicité ni pistage" to
                    "Tayyib n'intègre aucun SDK publicitaire ni outil de suivi comportemental et ne vend aucune donnée personnelle.",
                "Contact" to
                    "Pour toute question liée à la confidentialité : Muzamil Ahmad — Muzamilofficially@gmail.com."
            )
        )

        LegalDocumentType.TERMS_OF_USE -> LegalDocument(
            titleEn = "Terms of Use",
            titleFr = "Conditions d'utilisation",
            sectionsEn = listOf(
                "Acceptance" to
                    "By using Tayyib: Halal Food Scanner, you agree that all classifications, additive descriptions, and product suggestions are provided strictly as informational research aids.",
                "No Warranty or Certification" to
                    "Food manufacturers change recipes, suppliers, and factory lines without notice. Tayyib makes no guarantee of accuracy, completeness, or religious compliance. Always read the physical ingredient label before consuming a product.",
                "Independent Status" to
                    "Tayyib is not affiliated with, sponsored by, or endorsed by Halal Open Food Facts, Open Food Facts, JAKIM, BPJPH, IFANCA, or any food manufacturer.",
                "User Responsibility" to
                    "You are solely responsible for your dietary and allergen decisions. Tayyib is not a medical or allergen-safety tool."
            ),
            sectionsFr = listOf(
                "Acceptation" to
                    "En utilisant Tayyib : Scanner Alimentaire Halal, vous acceptez que les classifications, fiches d'additifs et suggestions d'alternatives soient fournies uniquement à titre d'aide informative.",
                "Absence de garantie ou de certification" to
                    "Les fabricants modifient régulièrement leurs recettes et fournisseurs sans préavis. Tayyib ne garantit pas l'exactitude ni la conformité religieuse des données. Vérifiez toujours l'étiquette physique avant consommation.",
                "Indépendance" to
                    "Tayyib n'est ni affilié, ni sponsorisé, ni approuvé par Halal Open Food Facts, Open Food Facts, JAKIM, BPJPH, IFANCA ou tout fabricant alimentaire.",
                "Responsabilité de l'utilisateur" to
                    "Vous demeurez seul responsable de vos choix alimentaires et de la gestion des allergènes."
            )
        )

        LegalDocumentType.HALAL_METHODOLOGY -> LegalDocument(
            titleEn = "Halal Classification Methodology",
            titleFr = "Méthodologie de classification Halal",
            sectionsEn = listOf(
                "Four Transparent Classifications" to
                    "• HALAL: Available evidence supports a Halal assessment under Tayyib's documented rules.\n• HARAM: Reliable evidence identifies a clearly prohibited ingredient (e.g., pork, lard, intoxicating alcohol, wine, blood).\n• DOUBTFUL: One or more ingredients (e.g., unspecified gelatin, E471, E120, animal rennet) or conflicting signals require source or certification verification.\n• UNKNOWN: Insufficient reliable ingredient or certification data exists. Missing information is NEVER classified as Haram.",
                "Key Religious Accuracy Rules" to
                    "• Missing data does not mean Haram.\n• Vegan or vegetarian labels do not automatically equal certified Halal (e.g., products may still contain alcohol or flavor solvents).\n• Absence of pork does not prove Halal certification.\n• E471, E472, E481, E482, E476, and E422 have multiple possible origins (plant or animal) and are flagged as source-dependent unless verified.\n• Gelatin (E441) requires source verification unless explicitly declared fish or certified Halal bovine.\n• E120 (Carmine/Cochineal) is transparently highlighted with scholarly nuance.",
                "Four Separate Assessment Pillars" to
                    "Every product displays: (1) Community Assessment from Halal Open Food Facts, (2) Tayyib Local Ingredient & E-Number Analysis, (3) Declared Label / Certification Information (distinguishing self-declared packaging labels from official registry checks), and (4) Final Application Assessment with evidence quality (High / Moderate / Limited / Insufficient).",
                "Rule Version" to
                    "Current bundled offline taxonomy rule version: v1.2.0."
            ),
            sectionsFr = listOf(
                "Quatre classifications transparentes" to
                    "• HALAL : Les éléments disponibles soutiennent une évaluation Halal selon notre méthodologie documentée.\n• HARAM : Des preuves fiables identifient un ingrédient clairement interdit (porc, saindoux, alcool, vin, sang).\n• DOUTEUX : Un ingrédient à origine variable (gélatine non précisée, E471, E120, présure) ou des indices contradictoires nécessitent une vérification.\n• INCONNU : Données insuffisantes. L'absence d'information n'est JAMAIS classée Haram.",
                "Règles clés de rigueur" to
                    "• Des données manquantes ne signifient pas Haram.\n• Les mentions végane ou végétarien ne prouvent pas automatiquement une certification Halal.\n• L'absence de porc ne prouve pas une certification Halal.\n• Les additifs E471, E472, E481, E482, E476 et E422 peuvent être d'origine végétale ou animale et sont signalés selon leur source.\n• La gélatine (E441) exige une vérification de son origine.\n• Le E120 (carmin) est expliqué de manière nuancée selon les avis savants.",
                "Quatre piliers distincts" to
                    "Chaque fiche distingue : (1) l'évaluation communautaire Halal Open Food Facts, (2) l'analyse locale des ingrédients et additifs E, (3) les mentions de certification déclarées (distinctes d'une vérification officielle en registre), et (4) l'évaluation finale avec son niveau de qualité de preuve.",
                "Version des règles" to
                    "Version actuelle de la taxonomie locale embarquée : v1.2.0."
            )
        )

        LegalDocumentType.DATA_ATTRIBUTION -> LegalDocument(
            titleEn = "Data Sources & Attribution",
            titleFr = "Sources de données et attribution",
            sectionsEn = listOf(
                "Primary & Automatic Backup Data Sources" to
                    "Primary: Halal Open Food Facts (https://halalopenfoodfacts.org/en/ — Docs: https://halalopenfoodfacts.org/api.html).\nAutomatic Live Backup: Open Food Facts official API (https://world.openfoodfacts.org/ — Docs: https://openfoodfacts.github.io/documentation/), queried automatically if Halal Open Food Facts is unreachable or missing a barcode.\nOffline Fallback: Local Room cache for previously viewed products.",
                "Open Database License (ODbL)" to
                    "Open Food Facts database contents are made available under the Open Database License (ODbL v1.0). Individual product images and packaging photos are licensed separately by contributors under Creative Commons Attribution-ShareAlike (CC BY-SA 3.0) or applicable product copyright terms.",
                "Certification Bodies Reference" to
                    "Tayyib references public guidance from internationally recognized certification bodies such as JAKIM (Malaysia), BPJPH (Indonesia), IFANCA (North America), and European certifiers for educational context. Tayyib does NOT claim direct live official certificate registry verification unless explicitly integrated and displayed.",
                "Independence Statement" to
                    "Tayyib is an independent application created by Muzamil Ahmad and is not an official app of, nor endorsed by, Halal Open Food Facts, Open Food Facts, or any certification authority."
            ),
            sectionsFr = listOf(
                "Source principale et serveur de secours automatique" to
                    "Source principale : Halal Open Food Facts (https://halalopenfoodfacts.org/en/ — Docs : https://halalopenfoodfacts.org/api.html).\nSecours automatique en direct : Open Food Facts (https://world.openfoodfacts.org/ — Docs : https://openfoodfacts.github.io/documentation/), interrogé automatiquement si Halal Open Food Facts est indisponible ou ne référence pas un code-barres.\nSecours hors ligne : cache local Room pour les produits déjà consultés.",
                "Licence Open Database License (ODbL)" to
                    "Le contenu de la base Open Food Facts est mis à disposition sous licence Open Database License (ODbL v1.0). Les photographies de produits relèvent de licences distinctes (CC BY-SA 3.0 ou droits applicables).",
                "Organismes de certification référencés" to
                    "Tayyib s'appuie à titre éducatif sur les référentiels publics d'organismes reconnus tels que JAKIM, BPJPH et IFANCA. Tayyib ne revendique pas de vérification directe en temps réel sur un registre officiel de certificats.",
                "Déclaration d'indépendance" to
                    "Tayyib est une application indépendante développée par Muzamil Ahmad et n'est ni officielle ni approuvée par Halal Open Food Facts, Open Food Facts ou un organisme certificateur."
            )
        )

        LegalDocumentType.OPEN_SOURCE_LICENSES -> LegalDocument(
            titleEn = "Open-Source Licenses",
            titleFr = "Licences open-source",
            sectionsEn = listOf(
                "Android Jetpack & Compose" to
                    "Jetpack Compose, Material 3, CameraX, Room, DataStore, Navigation Compose, Lifecycle — Apache License 2.0.",
                "Networking & Serialization" to
                    "Retrofit, OkHttp, Moshi — Apache License 2.0 (Square, Inc.).\nCoil Image Loader — Apache License 2.0.",
                "Barcode Scanning & Lens OCR" to
                    "Google ML Kit Barcode Scanning & ML Kit Text Recognition — Android SDK & ML Kit Terms of Service (on-device processing).",
                "Typography" to
                    "Inter & Plus Jakarta Sans — SIL Open Font License 1.1."
            ),
            sectionsFr = listOf(
                "Android Jetpack & Compose" to
                    "Jetpack Compose, Material 3, CameraX, Room, DataStore, Navigation Compose, Lifecycle — Licence Apache 2.0.",
                "Réseau et sérialisation" to
                    "Retrofit, OkHttp, Moshi — Licence Apache 2.0 (Square, Inc.).\nCoil — Licence Apache 2.0.",
                "Codes-barres et OCR Lens" to
                    "Google ML Kit Barcode Scanning & ML Kit Text Recognition — Traitement local sur appareil.",
                "Typographie" to
                    "Inter & Plus Jakarta Sans — Licence SIL Open Font 1.1."
            )
        )
    }
}
