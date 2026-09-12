package com.phms.app.data

data class SubCounty(
    val name: String,
    val wards: List<String>
)

data class County(
    val name: String,
    val subCounties: List<SubCounty>
)

object KenyaLocations {
    val counties: List<County> = listOf(
        County(
            name = "Mombasa",
            subCounties = listOf(
                SubCounty("Changamwe", listOf("Chaani", "Changamwe", "Kipevu", "Airport", "Port Reitz")),
                SubCounty("Jomvu", listOf("Jomvu Kuu", "Miritini", "Mikindani")),
                SubCounty("Kisauni", listOf("Mjambere", "Junda", "Bamburi", "Mwakirunge", "Mtopanga", "Magogoni", "Shanzu")),
                SubCounty("Nyali", listOf("Frere Town", "Ziwa La Ng'ombe", "Mkomani", "Kongowea", "Kadzandani")),
                SubCounty("Likoni", listOf("Mtongwe", "Shika Adabu", "Bofu", "Likoni", "Timbwani")),
                SubCounty("Mvita", listOf("Mji wa Kale/Makadara", "Tudor", "Tononoka", "Shimanzi/Ganjoni", "Majengo"))
            )
        ),
        County(
            name = "Nakuru",
            subCounties = listOf(
                SubCounty("Nakuru East", listOf("Biashara", "Kivumbini", "Flamingo", "Menengai", "Nakuru Town")),
                SubCounty("Nakuru West", listOf("Barut", "London", "Kapkures", "Rhoda", "Shaabab")),
                SubCounty("Naivasha", listOf("Biashara", "Hells Gate", "Lake View", "Mai Mahiu", "Maeilla", "Olkaria")),
                SubCounty("Gilgil", listOf("Gilgil", "Elementaita", "Mbaruk/Eburu", "Malewa West", "Murindat")),
                SubCounty("Molo", listOf("Molo", "Elburgon", "Tinet", "Kiamkururia")),
                SubCounty("Njoro", listOf("Njoro", "Lare", "Nessuit", "Pihari", "Kihingo"))
            )
        ),
        County(
            name = "Nairobi",
            subCounties = listOf(
                SubCounty("Westlands", listOf("Kitisuru", "Parklands/Highridge", "Karura", "Kangemi", "Mountain View")),
                SubCounty("Dagoretti North", listOf("Kilimani", "Kawangware", "Gatina", "Kileleshwa", "Lavington")),
                SubCounty("Dagoretti South", listOf("Mutu-ini", "Ngando", "Riruta", "Uthiru/Ruthimitu", "Waithaka")),
                SubCounty("Langata", listOf("Karen", "Nairobi West", "Mugumo-ini", "South C", "Nyayo Highrise")),
                SubCounty("Kibra", listOf("Laini Saba", "Lindi", "Makina", "Woodley/Kenyatta Golf", "Sarang'ombe")),
                SubCounty("Kasarani", listOf("Clay City", "Mwiki", "Kasarani", "Njiru", "Ruai")),
                SubCounty("Ruaraka", listOf("Babadogo", "Utalii", "Mathare North", "Lucky Summer", "Korogocho"))
            )
        ),
        County(
            name = "Kiambu",
            subCounties = listOf(
                SubCounty("Githunguri", listOf("Githunguri", "Gathanji", "Ikinu", "Ngewa", "Komothai")),
                SubCounty("Ruiru", listOf("Gitothua", "Biashara", "Gatongora", "Kahawa Sukari", "Kahawa Wendani", "Mwihoko")),
                SubCounty("Juja", listOf("Murera", "Theta", "Juja", "Kalimoni", "Witeithie")),
                SubCounty("Thika Town", listOf("Township", "Kamenu", "Hospital", "Gatuanyaga", "Ngoliba")),
                SubCounty("Kikuyu", listOf("Karai", "Nachu", "Sigona", "Kikuyu", "Kinoo")),
                SubCounty("Limuru", listOf("Bibirioni", "Limuru Central", "Ndeiya", "Limuru East", "Ngecha Fanaka"))
            )
        ),
        County(
            name = "Uasin Gishu",
            subCounties = listOf(
                SubCounty("Ainabkoi", listOf("Kapsoya", "Kaptagat", "Ainabkoi/Olare")),
                SubCounty("Kapseret", listOf("Simat/Kapseret", "Kipkenyo", "Ngeria", "Megun", "Langas")),
                SubCounty("Kesses", listOf("Racecourse", "Cheptiret/Kipchamo", "Tulwet/Chuiyat", "Tarakwa")),
                SubCounty("Moiben", listOf("Tembelio", "Sergoit", "Karcin", "Moiben", "Kimumu")),
                SubCounty("Soy", listOf("Moi's Bridge", "Kapkenda", "Ziwa", "Segero/Barsombe", "Kiplombe")),
                SubCounty("Turbo", listOf("Ngenyilel", "Tapsagoi", "Kamagut", "Kiplombe", "Kapsaos", "Huruma"))
            )
        ),
        County(
            name = "Kakamega",
            subCounties = listOf(
                SubCounty("Lurambi", listOf("Butsotso East", "Butsotso South", "Butsotso Central", "Sheywe", "Mahiakalo", "Shirere")),
                SubCounty("Malava", listOf("West Kabras", "Chemuche", "East Kabras", "South Kabras", "Manda-Shivanga")),
                SubCounty("Mumias West", listOf("Mumias Central", "Mumias North", "Etenje", "Musanda")),
                SubCounty("Mumias East", listOf("Lusheya/Lubinu", "Malaha/Isongo", "East Wanga"))
            )
        ),
        County(
            name = "Kisumu",
            subCounties = listOf(
                SubCounty("Kisumu Central", listOf("Railways", "Migosi", "Shaurimoyo Kaloleni", "Market Milimani", "Kondele")),
                SubCounty("Kisumu East", listOf("Kajulu", "Kolwa East", "Manyatta B", "Nyalenda A", "Kolwa Central")),
                SubCounty("Kisumu West", listOf("South West Kisumu", "Central Kisumu", "Kisumu North", "West Seme"))
            )
        ),
        County(
            name = "Kilifi",
            subCounties = listOf(
                SubCounty("Kilifi North", listOf("Tezo", "Sokoni", "Kibarani", "Dabaso", "Matsangoni", "Watamu")),
                SubCounty("Kilifi South", listOf("Junju", "Mwarakaya", "Shasimani", "Chonyi", "Pingilikani")),
                SubCounty("Malindi", listOf("Jilore", "Kakuyuni", "Ganda", "Malindi Town", "Shella"))
            )
        ),
        County(
            name = "Machakos",
            subCounties = listOf(
                SubCounty("Machakos Town", listOf("Kalama", "Muputi", "Machakos Central", "Muvuti/Kiima-Kimwe")),
                SubCounty("Mavoko", listOf("Athiriver", "Syokimau/Mulolongo", "Kinanie", "Muthwani")),
                SubCounty("Kangundo", listOf("Kangundo North", "Kangundo Central", "Kangundo East", "Kangundo West"))
            )
        ),
        County(
            name = "Nyeri",
            subCounties = listOf(
                SubCounty("Nyeri Central", listOf("Kiganjo/Mathari", "Rware", "Mukaro")),
                SubCounty("Mathira East", listOf("Iriaini", "Karatina Town", "Magutu")),
                SubCounty("Tetul", listOf("Aguthi-Gaaki", "Dedan Kimathi", "Kamakwa/Mukaro"))
            )
        ),
        County(
            name = "Meru",
            subCounties = listOf(
                SubCounty("Imenti North", listOf("Municipality", "Ntima East", "Ntima West", "Nyaki North", "Nyaki South")),
                SubCounty("Imenti South", listOf("Mitunguu", "Igoji East", "Igoji West", "Abogeta East", "Nkuene"))
            )
        )
    )

    fun getCountyNames(): List<String> = counties.map { it.name }

    fun getSubCountyNames(countyName: String): List<String> {
        val county = counties.find { it.name.equals(countyName, ignoreCase = true) }
        return county?.subCounties?.map { it.name } ?: emptyList()
    }

    fun getWardNames(countyName: String, subCountyName: String): List<String> {
        val county = counties.find { it.name.equals(countyName, ignoreCase = true) }
        val subCounty = county?.subCounties?.find { it.name.equals(subCountyName, ignoreCase = true) }
        return subCounty?.wards ?: emptyList()
    }
}
