package com.clawstack.shellguard.domain.models

/**
 * Bidirectional projection between [IdentityDetail] and [PearlDetail] (type = "identity").
 * Maintains 1:1 wire parity with ShellGuard Web (`ItemFormModal.tsx`, `ItemDetailPane.tsx`, `bitwarden.ts`).
 */
object IdentityFieldMapper {

    private val CANONICAL_KEYS = setOf(
        "title",
        "first name",
        "middle name",
        "last name",
        "full name",
        "username",
        "company",
        "email",
        "phone",
        "address 1", "address1",
        "address 2", "address2",
        "address 3", "address3",
        "city",
        "state / province", "state",
        "postal code", "zip",
        "country",
        "location",
        "ssn",
        "passport number", "passport",
        "license number", "license"
    )

    fun fromPearlDetail(pearl: PearlDetail): IdentityDetail {
        val fields = pearl.customFields
        fun findVal(vararg names: String): String {
            val lowerNames = names.map { it.lowercase() }.toSet()
            return fields.firstOrNull { it.label.trim().lowercase() in lowerNames }?.value?.trim().orEmpty()
        }

        val firstName = findVal("first name")
        val middleName = findVal("middle name")
        val lastName = findVal("last name")
        val computedFullName = listOf(firstName, middleName, lastName)
            .filter { it.isNotEmpty() }
            .joinToString(" ")
        val cfUsername = findVal("username")
        val resolvedUsername = cfUsername.ifBlank {
            if (pearl.username != computedFullName) pearl.username else ""
        }

        val extraCustomFields = fields.filter { it.label.trim().lowercase() !in CANONICAL_KEYS }

        return IdentityDetail(
            id = pearl.id,
            ownerUuid = pearl.ownerUuid,
            title = pearl.title,
            honorificTitle = findVal("title"),
            firstName = firstName,
            middleName = middleName,
            lastName = lastName,
            username = resolvedUsername,
            company = findVal("company"),
            email = findVal("email"),
            phone = findVal("phone"),
            address1 = findVal("address 1", "address1"),
            address2 = findVal("address 2", "address2"),
            address3 = findVal("address 3", "address3"),
            city = findVal("city"),
            state = findVal("state / province", "state"),
            postalCode = findVal("postal code", "zip"),
            country = findVal("country"),
            ssn = findVal("ssn"),
            passportNumber = findVal("passport number", "passport"),
            licenseNumber = findVal("license number", "license"),
            category = pearl.category,
            notes = pearl.notes,
            customFields = extraCustomFields,
            attachments = pearl.attachments,
            tags = pearl.tags,
            reprompt = pearl.reprompt,
            syncState = pearl.syncState,
            createdAt = pearl.createdAt,
            localUpdatedAt = pearl.localUpdatedAt,
            remoteUpdatedAt = pearl.remoteUpdatedAt
        )
    }

    fun toPearlDetail(identity: IdentityDetail): PearlDetail {
        val fullName = identity.fullName
        val location = identity.location
        val trimmedUser = identity.username.trim()
        val trimmedNotes = identity.notes.trim()
        val trimmedTitle = identity.title.trim()

        val serializedFields = mutableListOf<CustomField>()
        fun addText(name: String, raw: String) {
            val v = raw.trim()
            if (v.isNotEmpty()) {
                serializedFields.add(CustomField(label = name, value = v, type = CustomFieldType.TEXT))
            }
        }
        fun addHidden(name: String, raw: String) {
            val v = raw.trim()
            if (v.isNotEmpty()) {
                serializedFields.add(CustomField(label = name, value = v, type = CustomFieldType.HIDDEN))
            }
        }

        addText("Title", identity.honorificTitle)
        addText("First Name", identity.firstName)
        addText("Middle Name", identity.middleName)
        addText("Last Name", identity.lastName)
        addText("Full Name", fullName)
        addText("Username", trimmedUser)
        addText("Company", identity.company)
        addText("Email", identity.email)
        addText("Phone", identity.phone)
        addText("Address 1", identity.address1)
        addText("Address 2", identity.address2)
        addText("Address 3", identity.address3)
        addText("City", identity.city)
        addText("State / Province", identity.state)
        addText("Postal Code", identity.postalCode)
        addText("Country", identity.country)
        addText("Location", location)
        addHidden("SSN", identity.ssn)
        addHidden("Passport Number", identity.passportNumber)
        addHidden("License Number", identity.licenseNumber)

        // Preserve any user-defined custom fields without duplicating canonical keys
        identity.customFields
            .filter { it.label.trim().lowercase() !in CANONICAL_KEYS }
            .forEach { serializedFields.add(it) }

        val finalSecret = trimmedNotes
            .ifBlank { fullName }
            .ifBlank { trimmedUser }
            .ifBlank { trimmedTitle }
            .ifBlank { "Identity" }

        val finalUsername = trimmedUser.ifBlank { fullName }

        return PearlDetail(
            id = identity.id,
            ownerUuid = identity.ownerUuid,
            title = trimmedTitle.ifBlank { "Untitled Identity" },
            secret = finalSecret,
            username = finalUsername,
            url = "",
            type = "identity",
            category = identity.category.trim(),
            notes = trimmedNotes,
            totpSecret = "",
            customFields = serializedFields,
            uris = emptyList(),
            attachments = identity.attachments,
            tags = identity.tags.ifEmpty { listOf("Identity") },
            passwordHistory = emptyList(),
            reprompt = identity.reprompt,
            syncState = identity.syncState,
            createdAt = identity.createdAt,
            localUpdatedAt = identity.localUpdatedAt,
            remoteUpdatedAt = identity.remoteUpdatedAt
        )
    }
}
