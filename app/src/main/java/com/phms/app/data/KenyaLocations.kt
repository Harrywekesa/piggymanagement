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
        // 001 Mombasa
        County("Mombasa", listOf(
            SubCounty("Changamwe", listOf("Chaani", "Changamwe", "Kipevu", "Airport", "Port Reitz")),
            SubCounty("Jomvu", listOf("Jomvu Kuu", "Miritini", "Mikindani")),
            SubCounty("Kisauni", listOf("Mjambere", "Junda", "Bamburi", "Mwakirunge", "Mtopanga", "Magogoni", "Shanzu")),
            SubCounty("Nyali", listOf("Frere Town", "Ziwa La Ng'ombe", "Mkomani", "Kongowea", "Kadzandani")),
            SubCounty("Likoni", listOf("Mtongwe", "Shika Adabu", "Bofu", "Likoni", "Timbwani")),
            SubCounty("Mvita", listOf("Mji wa Kale/Makadara", "Tudor", "Tononoka", "Shimanzi/Ganjoni", "Majengo"))
        )),
        // 002 Kwale
        County("Kwale", listOf(
            SubCounty("Msambweni", listOf("Gombato Bongwe", "Ukunda", "Kinondo", "Ramisi")),
            SubCounty("Lungalunga", listOf("Pongwe/Kikoneni", "Dzombo", "Mwereni", "Vanga")),
            SubCounty("Matuga", listOf("Tsimba Golini", "Waa", "Tiwi", "Kubo South", "Shimba Hills")),
            SubCounty("Kinango", listOf("Ndavaya", "Puma", "Kinango", "Chengoni/Samburu", "Mackinnon Road", "Chonyi", "Mwavumbo", "Kasemeni"))
        )),
        // 003 Kilifi
        County("Kilifi", listOf(
            SubCounty("Kilifi North", listOf("Tezo", "Sokoni", "Kibarani", "Dabaso", "Matsangoni", "Watamu", "Mnarani")),
            SubCounty("Kilifi South", listOf("Junju", "Mwarakaya", "Shasimani", "Chonyi", "Pingilikani")),
            SubCounty("Kaloleni", listOf("Mariakani", "Kayafungo", "Kaloleni", "Mwanamwinga")),
            SubCounty("Rabai", listOf("Mwawesa", "Ruruma", "Kambe/Ribe", "Rabai/Kisurutini")),
            SubCounty("Ganze", listOf("Ganze", "Bamba", "Jaribuni", "Sokoke")),
            SubCounty("Malindi", listOf("Jilore", "Kakuyuni", "Ganda", "Malindi Town", "Shella")),
            SubCounty("Magarini", listOf("Marafa", "Magarini", "Gongoni", "Adu", "Garashi", "Sabaki"))
        )),
        // 004 Tana River
        County("Tana River", listOf(
            SubCounty("Garsen", listOf("Kipini East", "Garsen South", "Kipini West", "Garsen West", "Garsen Central", "Garsen North")),
            SubCounty("Galole", listOf("Kinakomba", "Mikinduni", "Chewele", "Wayu")),
            SubCounty("Bura", listOf("Bangale", "Sala", "Madogo", "Tana North", "Tana Delta"))
        )),
        // 005 Lamu
        County("Lamu", listOf(
            SubCounty("Lamu East", listOf("Faza", "Kiunga", "Basuba")),
            SubCounty("Lamu West", listOf("Shella", "Mkomani", "Hindi", "Mkunumbi", "Hongwe", "Witu", "Bahari"))
        )),
        // 006 Taita Taveta
        County("Taita Taveta", listOf(
            SubCounty("Taveta", listOf("Chala", "Mahoo", "Bomani", "Mboghoni", "Mata")),
            SubCounty("Wundanyi", listOf("Wundanyi/Mbale", "Werugha", "Wumingu/Kishushe", "Mwanda/Mgange")),
            SubCounty("Mwatate", listOf("Rong'e", "Mwatate", "Bura", "Chawia", "Wusi/Kishamba")),
            SubCounty("Voi", listOf("Mbololo", "Sagalla", "Ngerenyi/Mraru", "Kishamba", "Marungu"))
        )),
        // 007 Garissa
        County("Garissa", listOf(
            SubCounty("Garissa Township", listOf("Waberi", "Galbet", "Township", "Iftin")),
            SubCounty("Balambala", listOf("Balambala", "Danyere", "Jarajila", "Goreale", "Malkadida")),
            SubCounty("Lagdera", listOf("Modogashe", "Benane", "Goreale", "Maalimin", "Sabena")),
            SubCounty("Dadaab", listOf("Dertu", "Dagahaley", "Liboi", "Lagodera")),
            SubCounty("Fafi", listOf("Nanighi", "Bura", "Dekaharia", "Jarajila")),
            SubCounty("Ijara", listOf("Sangailu", "Ijara", "Masalani", "Hulugho"))
        )),
        // 008 Wajir
        County("Wajir", listOf(
            SubCounty("Wajir North", listOf("Gurar", "Bute", "Korondille", "Malkagufu", "Batalu", "Danaba")),
            SubCounty("Wajir East", listOf("Wagberi", "Township", "Barwaqo", "Khorof/Harar")),
            SubCounty("Tarbaj", listOf("Tarbaj", "Wargadud", "Lagbogol", "Ganyure/Wagalla")),
            SubCounty("Wajir West", listOf("Githiorioni", "Hadado/Athibaa", "Ademasajida")),
            SubCounty("Eldas", listOf("Eldas", "Della", "Lakoley South/Basir")),
            SubCounty("Wajir South", listOf("Alinjugur", "Arabian", "Benane", "Sarman", "Elben"))
        )),
        // 009 Mandera
        County("Mandera", listOf(
            SubCounty("Mandera West", listOf("Takaba South", "Takaba", "Lafey", "Fino", "Khalalio", "Neboi")),
            SubCounty("Mandera North", listOf("Ashabito", "Guticha", "Morothile", "Rhamu", "Rhamu Dimtu")),
            SubCounty("Mandera South", listOf("Wargadud", "Hareri", "Ganda", "Malkamari")),
            SubCounty("Mandera East", listOf("Libehia", "Warankara", "Lagsure", "Dandu")),
            SubCounty("Banissa", listOf("Banissa", "Derkhale", "Gari", "Malkamari", "Kiliwehiri")),
            SubCounty("Mandera Central", listOf("Arabian", "Neboi", "Township", "Khalalio"))
        )),
        // 010 Marsabit
        County("Marsabit", listOf(
            SubCounty("Moyale", listOf("Moyale Township", "Butiye", "Sololo", "Golbo")),
            SubCounty("North Horr", listOf("Dukana", "Maikona", "Turbi", "North Horr", "Illeret")),
            SubCounty("Saku", listOf("Marsabit Central", "Sagante/Jaldesa", "Karare")),
            SubCounty("Laisamis", listOf("Loiyangalani", "Kargi/South Horr", "Korr/Ngurunit", "Logologo", "Laisamis"))
        )),
        // 011 Isiolo
        County("Isiolo", listOf(
            SubCounty("Isiolo North", listOf("Wabera", "Bulla Pesa", "Chari", "Cherab", "Oldonyiro")),
            SubCounty("Isiolo South", listOf("Garbatulla", "Kina", "Sericho"))
        )),
        // 012 Meru
        County("Meru", listOf(
            SubCounty("Imenti North", listOf("Municipality", "Ntima East", "Ntima West", "Nyaki North", "Nyaki South")),
            SubCounty("Imenti Central", listOf("Mwimbi", "Muthambi", "Igoji East", "Igoji West")),
            SubCounty("Imenti South", listOf("Mitunguu", "Abogeta East", "Abogeta West", "Nkuene")),
            SubCounty("Tigania West", listOf("Akachiu", "Kanuni", "Kianjai", "Ngage", "Mbeu")),
            SubCounty("Tigania East", listOf("Thangatha", "Mikinduri", "Ganga", "Mutuati", "Athwana")),
            SubCounty("Igembe North", listOf("Antuambui", "Ntunene", "Antubetwe Kiongo", "Naathu")),
            SubCounty("Igembe Central", listOf("Njia", "Kangeta", "Igembe", "Athiru Gaiti")),
            SubCounty("Igembe South", listOf("Maua", "Kiegoi/Antubochiu", "Athiru Ruujine", "Mariene", "Lare"))
        )),
        // 013 Tharaka Nithi
        County("Tharaka Nithi", listOf(
            SubCounty("Maara", listOf("Mwimbi", "Muthambi", "Ganga", "Chogoria", "Mugumoni")),
            SubCounty("Chuka/Igambang'ombe", listOf("Mariani", "Karingani", "Magumoni", "Mugwe")),
            SubCounty("Tharaka North", listOf("Gatunga", "Mukothima")),
            SubCounty("Tharaka South", listOf("Nkondi", "Chiakariga", "Marimanti"))
        )),
        // 014 Embu
        County("Embu", listOf(
            SubCounty("Mbeere North", listOf("Evurore", "Kiambere", "Mavuria", "Mbeti North")),
            SubCounty("Mbeere South", listOf("Nthawa", "Mwea", "Makima", "Mbeti South", "Nthawa")),
            SubCounty("Embu West", listOf("Ngandori/Nginda", "Kanjeru", "Gaturi North", "Gaturi South")),
            SubCounty("Embu East", listOf("Kagaari South", "Central", "Kagaari North", "Kyeni North", "Kyeni South")),
            SubCounty("Embu North", listOf("Ruguru", "Kirimari", "Murugi/Mukinduri"))
        )),
        // 015 Kitui
        County("Kitui", listOf(
            SubCounty("Kitui West", listOf("Mutonguni", "Kauwi", "Matinyani", "Kwa Mutonga/Kithumula")),
            SubCounty("Kitui Rural", listOf("Kisasi", "Mwitika", "Nzambani", "Tseikuru")),
            SubCounty("Kitui Central", listOf("Township", "Miambani", "Hospital", "Kyangwithya West")),
            SubCounty("Kitui East", listOf("Zombe/Mwitika", "Shambani", "Mutito/Kaliku", "Nzooni")),
            SubCounty("Kitui South", listOf("Ikutha", "Mutomo", "Miambani", "Ngomeni", "Mutha")),
            SubCounty("Mwingi North", listOf("Kyuso", "Mumoni", "Tharaka", "Ngomeni", "Mui")),
            SubCounty("Mwingi West", listOf("Mwingi Central", "Kivou", "Nguni", "Nuu", "Mui")),
            SubCounty("Mwingi Central", listOf("Mwingi Town", "Central", "Kivou", "Kyome/Thaana"))
        )),
        // 016 Machakos
        County("Machakos", listOf(
            SubCounty("Machakos Town", listOf("Kalama", "Muputi", "Machakos Central", "Muvuti/Kiima Kimwe", "Mutituni")),
            SubCounty("Mavoko", listOf("Athiriver", "Syokimau/Mulolongo", "Kinanie", "Muthwani")),
            SubCounty("Matungulu", listOf("Matungulu North", "Matungulu West", "Matungulu East", "Kyeleni")),
            SubCounty("Kathiani", listOf("Mitaboni", "Kathiani Central", "Upper Mwau", "Iiani")),
            SubCounty("Masinga", listOf("Masinga Central", "Ekalakala", "Muthesya", "Ndithini")),
            SubCounty("Yatta", listOf("Ndithini", "Ikombe", "Katangi", "Yatta", "Mulutu", "Kalama")),
            SubCounty("Kangundo", listOf("Kangundo North", "Kangundo Central", "Kangundo East", "Kangundo West")),
            SubCounty("Mwala", listOf("Mbiuni", "Makutano/Mwala", "Kibauni", "Selengei", "Muthetheni"))
        )),
        // 017 Makueni
        County("Makueni", listOf(
            SubCounty("Makueni", listOf("Wote", "Muvau/Kikumini", "Mavindini", "Kilungu")),
            SubCounty("Kaiti", listOf("Mukaa", "Kiima Kimwe/Kalanzoni", "Kee", "Kasikeu")),
            SubCounty("Mbooni", listOf("Mbooni", "Kithungo/Kitundu", "Kisau/Kiteta")),
            SubCounty("Kibwezi West", listOf("Emali/Mulala", "Nguu/Masumba", "Mukaa", "Nziu")),
            SubCounty("Kibwezi East", listOf("Kibwezi", "Masongaleni", "Mtito Andei", "Thange", "Ivingoni/Nzambani"))
        )),
        // 018 Nyandarua
        County("Nyandarua", listOf(
            SubCounty("Kinangop", listOf("Engineer", "Gathara", "North Kinangop", "Murungaru")),
            SubCounty("Kipipiri", listOf("Geta", "Githioro", "Kipipiri")),
            SubCounty("Ol Kalou", listOf("Karau", "Kanjuiri Ridge", "Mirangine", "Kaimbaga", "Sulguta")),
            SubCounty("Ol Joro Orok", listOf("Githioro", "Wanjohi", "Shamata")),
            SubCounty("Ndaragwa", listOf("Leshau", "Pondo", "Rurii"))
        )),
        // 019 Nyeri
        County("Nyeri", listOf(
            SubCounty("Tetu", listOf("Dedan Kimathi", "Aguthi/Gaaki", "Kamakwa/Mukaro", "Iria-ini", "Chinga")),
            SubCounty("Kieni", listOf("Mweiga", "Naromoru/Kiamathaga", "Ngobit", "Mwiyogo/Endarasha", "Mukurwe-ini East")),
            SubCounty("Mathira East", listOf("Iriaini", "Karatina Town", "Magutu")),
            SubCounty("Mathira West", listOf("Konyu", "Kagumo", "Gikondi")),
            SubCounty("Nyeri Central", listOf("Kiganjo/Mathari", "Rware", "Mukaro/Aguthi")),
            SubCounty("Mukurwe-ini", listOf("Giathugu", "Mukurwe-ini West", "Kabaru", "Aguthi")),
            SubCounty("Othaya", listOf("Iriti", "Mahiga", "Mweiga", "Karima/Ichagaki"))
        )),
        // 020 Kirinyaga
        County("Kirinyaga", listOf(
            SubCounty("Mwea", listOf("Mutithi", "Kangai", "Wamumu", "Nguka", "Thiba", "Tebere")),
            SubCounty("Gichugu", listOf("Kabare", "Kerugoya", "Inoi", "Baragwi")),
            SubCounty("Ndia", listOf("Mukure", "Kiine", "Karumandi")),
            SubCounty("Kirinyaga Central", listOf("Mutira", "Kanyekini", "Kerugoya", "Kutus"))
        )),
        // 021 Murang'a
        County("Murang'a", listOf(
            SubCounty("Kangema", listOf("Kanyenyaini", "Muguru", "Rwathia")),
            SubCounty("Mathioya", listOf("Kamacharia", "Gitugi", "Kiru")),
            SubCounty("Kiharu", listOf("Wangu", "Mugoiri", "Mbiri", "Township", "Murarandia")),
            SubCounty("Kigumo", listOf("Kahumbu", "Muthithi", "Kigumo", "Kangari")),
            SubCounty("Maragwa", listOf("Kimorori/Wempa", "Makuyu", "Kambiti", "Kamahuha", "Ichagaki")),
            SubCounty("Kandara", listOf("Ng'araria", "Muruka", "Kagundu-ini", "Gaichanjiru", "Ithiru")),
            SubCounty("Gatanga", listOf("Ithanga", "Kakuzi/Mitubiri", "Mugumo-ini", "Kihumbu-ini", "Gatanga"))
        )),
        // 022 Kiambu
        County("Kiambu", listOf(
            SubCounty("Githunguri", listOf("Githunguri", "Gathanji", "Ikinu", "Ngewa", "Komothai")),
            SubCounty("Ruiru", listOf("Gitothua", "Biashara", "Gatongora", "Kahawa Sukari", "Kahawa Wendani", "Mwihoko")),
            SubCounty("Juja", listOf("Murera", "Theta", "Juja", "Kalimoni", "Witeithie")),
            SubCounty("Thika Town", listOf("Township", "Kamenu", "Hospital", "Gatuanyaga", "Ngoliba")),
            SubCounty("Kikuyu", listOf("Karai", "Nachu", "Sigona", "Kikuyu", "Kinoo")),
            SubCounty("Limuru", listOf("Bibirioni", "Limuru Central", "Ndeiya", "Limuru East", "Ngecha Fanaka")),
            SubCounty("Lari", listOf("Kijabe", "Nyanduma", "Kirenga", "Lari/Kirenga", "Kinale")),
            SubCounty("Kabete", listOf("Gitaru", "Muguga", "Nyadhuna/Kahuho", "Kabete", "Uthiru")),
            SubCounty("Kiambaa", listOf("Ndenderu", "Kihara", "Cianda")),
            SubCounty("Gatundu North", listOf("Gachika", "Igarro", "Githobokoni", "Chania")),
            SubCounty("Gatundu South", listOf("Kiganjo", "Ndarugu", "Ngenda", "Kiamwangi"))
        )),
        // 023 Turkana
        County("Turkana", listOf(
            SubCounty("Turkana North", listOf("Lapur", "Kaaleng/Kaikor", "Kibish", "Kataboi")),
            SubCounty("Turkana West", listOf("Kakuma", "Lopur", "Letea", "Songot", "Kalobeyei", "Lokichoggio")),
            SubCounty("Turkana Central", listOf("Kalokol", "Lodwar Township", "Kanamkemer", "Kotaruk/Lobei", "Turkwel")),
            SubCounty("Loima", listOf("Lokiriama/Lorengippi", "Turkwel", "Loima", "Pelekech")),
            SubCounty("Turkana South", listOf("Kerio", "Kainuk", "Kweria", "Kapedo/Napeitom")),
            SubCounty("Turkana East", listOf("Lokori/Kochodin", "Katilu", "Kaputir"))
        )),
        // 024 West Pokot
        County("West Pokot", listOf(
            SubCounty("Pokot South", listOf("Sook", "Taposa", "Lelan", "Batei")),
            SubCounty("Pokot North", listOf("Kacheliba", "Kodich", "Kasei", "Alale")),
            SubCounty("West Pokot", listOf("Kapenguria", "Siyoi", "Endugh", "Sook")),
            SubCounty("Pokot Central", listOf("Chepareria", "Batei", "Weiwei", "Riwo"))
        )),
        // 025 Samburu
        County("Samburu", listOf(
            SubCounty("Samburu West", listOf("Suguta Marmar", "Maralal", "Loosuk", "Poro", "Lolkuniani")),
            SubCounty("Samburu North", listOf("Nachola", "Ndoto", "Nyiro", "Angata Nanyuki")),
            SubCounty("Samburu East", listOf("Waso", "Wamba West", "Wamba North", "Wamba East"))
        )),
        // 026 Trans Nzoia
        County("Trans Nzoia", listOf(
            SubCounty("Kwanza", listOf("Kapomboi", "Kwanza", "Keiyo", "Bidii")),
            SubCounty("Endebess", listOf("Endebess", "Chepchoina", "Matumbei")),
            SubCounty("Saboti", listOf("Kinyoro", "Matisi", "Tuwani", "Saboti", "Machewa")),
            SubCounty("Kiminini", listOf("Kiminini", "Waitaluk", "Sirenda", "Nabiswa")),
            SubCounty("Cherangany", listOf("Sinyerere", "Makutano", "Kaplamai", "Motosiet", "Cherangany/Suwerwa"))
        )),
        // 027 Uasin Gishu
        County("Uasin Gishu", listOf(
            SubCounty("Ainabkoi", listOf("Kapsoya", "Kaptagat", "Ainabkoi/Olare")),
            SubCounty("Kapseret", listOf("Simat/Kapseret", "Kipkenyo", "Ngeria", "Megun", "Langas")),
            SubCounty("Kesses", listOf("Racecourse", "Cheptiret/Kipchamo", "Tulwet/Chuiyat", "Tarakwa")),
            SubCounty("Moiben", listOf("Tembelio", "Sergoit", "Krcin", "Moiben", "Kimumu")),
            SubCounty("Soy", listOf("Moi's Bridge", "Kapkenda", "Ziwa", "Segero/Barsombe", "Kiplombe")),
            SubCounty("Turbo", listOf("Ngenyilel", "Tapsagoi", "Kamagut", "Huruma", "Kapsaos"))
        )),
        // 028 Elgeyo Marakwet
        County("Elgeyo Marakwet", listOf(
            SubCounty("Keiyo North", listOf("Emsoo", "Kamariny", "Metkei", "Soy North")),
            SubCounty("Keiyo South", listOf("Kaptiony", "Soy South", "Kapcherop", "Lelan")),
            SubCounty("Marakwet East", listOf("Embobut/Embolot", "Endo", "Cherangany")),
            SubCounty("Marakwet West", listOf("Kapyego", "Sambirir", "Arror", "Chebiemit"))
        )),
        // 029 Nandi
        County("Nandi", listOf(
            SubCounty("Chesumei", listOf("Lelmokwo/Ngechek", "Chemundu/Kapng'etuny", "Kosirai", "Chepkumia")),
            SubCounty("Emgwen", listOf("Chepterwai", "Kiptuya", "Nandi Hills", "Kapsimotwa")),
            SubCounty("Mosop", listOf("Kabiyet", "Ndalat", "Kurgung/Surungai", "Kapsabet", "Kabisaga")),
            SubCounty("Nandi Hills", listOf("Chepkumia", "Kapsimotwa", "Kilibwoni", "Kabiyet")),
            SubCounty("Aldai", listOf("Kabwareng", "Terik", "Kemeloi-Maraba", "Kobujoi", "Kaptumo/Kaboi"))
        )),
        // 030 Baringo
        County("Baringo", listOf(
            SubCounty("Baringo Central", listOf("Kabarnet", "Sacho", "Tenges", "Kapropita")),
            SubCounty("Baringo North", listOf("Barwessa", "Kabartonjo", "Saimo/Kipsaraman", "Saimo/Soi", "Bartabwa")),
            SubCounty("Baringo South", listOf("Mukutani", "Marigat", "Ilchamus", "Mochongoi")),
            SubCounty("Koibatek", listOf("Lembus", "Lembus Kwen", "Ravine", "Mumberes/Maji Mazuri", "Lembus Perkerra")),
            SubCounty("Mogotio", listOf("Mogotio", "Emining", "Kisanana")),
            SubCounty("Eldama Ravine", listOf("Ravine", "Koibatek", "Mumberes/Maji Mazuri", "Lembus East"))
        )),
        // 031 Laikipia
        County("Laikipia", listOf(
            SubCounty("Laikipia East", listOf("Ngobit", "Tigithi", "Thingithu", "Nanyuki", "Umande")),
            SubCounty("Laikipia West", listOf("Ol Moran", "Rumuruti Township", "Githiga", "Marmanet", "Igwamiti")),
            SubCounty("Laikipia North", listOf("Mukogondo East", "Mukogondo West"))
        )),
        // 032 Nakuru
        County("Nakuru", listOf(
            SubCounty("Nakuru East", listOf("Biashara", "Kivumbini", "Flamingo", "Menengai", "Nakuru Town")),
            SubCounty("Nakuru West", listOf("Barut", "London", "Kapkures", "Rhoda", "Shaabab")),
            SubCounty("Naivasha", listOf("Biashara", "Hells Gate", "Lake View", "Mai Mahiu", "Maeilla", "Olkaria")),
            SubCounty("Gilgil", listOf("Gilgil", "Elementaita", "Mbaruk/Eburu", "Malewa West", "Murindat")),
            SubCounty("Molo", listOf("Molo", "Elburgon", "Tinet", "Kiamkururia")),
            SubCounty("Njoro", listOf("Njoro", "Lare", "Nessuit", "Pihari", "Kihingo")),
            SubCounty("Kuresoi North", listOf("Kuresoi", "Sirikwa", "Kamwaura")),
            SubCounty("Kuresoi South", listOf("Amalo", "Keringet", "Kiptagich")),
            SubCounty("Subukia", listOf("Subukia", "Waseges", "Kabazi")),
            SubCounty("Rongai", listOf("Menengai West", "Soin", "Visoi", "Mosop", "Solai")),
            SubCounty("Bahati", listOf("Bahati", "Dundori", "Kabatini", "Kiamaina", "Lanet/Umoja"))
        )),
        // 033 Kajiado
        County("Kajiado", listOf(
            SubCounty("Kajiado North", listOf("Purko", "Rongai", "Olkeri", "Nkaimurunya")),
            SubCounty("Kajiado Central", listOf("Kimana", "Ilkisonko", "Kajiado Central")),
            SubCounty("Kajiado East", listOf("Mosiro", "Iloodokilani", "Mailua")),
            SubCounty("Kajiado West", listOf("Keekonyokie", "Iloitokitok", "Magadi", "Ewaso Oo Nkidong'i")),
            SubCounty("Loitokitok", listOf("Iloitokitok", "Rombo", "Kuku", "Imbirikani/Entonet", "Amboseli"))
        )),
        // 034 Kericho
        County("Kericho", listOf(
            SubCounty("Soin/Sigowet", listOf("Kabianga", "Soin", "Sigowet")),
            SubCounty("Kipkelion East", listOf("Chepseon", "Roret", "Kipsitet", "Londiani", "Tendeno/Sorget")),
            SubCounty("Kipkelion West", listOf("Chilchila", "Kunyak", "Kipkelion")),
            SubCounty("Ainamoi", listOf("Kericho East", "Ainamoi", "Kipchebor", "Ndanai/Abosi")),
            SubCounty("Bureti", listOf("Tebesonik", "Chemosot", "Litein", "Cheplanget", "Kapkatet")),
            SubCounty("Belgut", listOf("Kabianga", "Cheptarit", "Waldai", "Kapsuser"))
        )),
        // 035 Bomet
        County("Bomet", listOf(
            SubCounty("Bomet Central", listOf("Silibwet Township", "Ndaraweta", "Singorwet", "Chesoen")),
            SubCounty("Bomet East", listOf("Merigi", "Kembu", "Longisa", "Kipreres")),
            SubCounty("Chepalungu", listOf("Sigor", "Mogogosiek", "Rongena/Manaret")),
            SubCounty("Konoin", listOf("Mautuma", "Boito", "Embomos", "Chepchabas")),
            SubCounty("Sotik", listOf("Ndanai/Abosi", "Chemagel", "Kong'asis", "Nkonkia", "Merigi"))
        )),
        // 036 Kakamega
        County("Kakamega", listOf(
            SubCounty("Lurambi", listOf("Butsotso East", "Butsotso South", "Butsotso Central", "Sheywe", "Mahiakalo", "Shirere")),
            SubCounty("Malava", listOf("West Kabras", "Chemuche", "East Kabras", "South Kabras", "Manda-Shivanga")),
            SubCounty("Mumias West", listOf("Mumias Central", "Mumias North", "Etenje", "Musanda")),
            SubCounty("Mumias East", listOf("Lusheya/Lubinu", "Malaha/Isongo", "East Wanga")),
            SubCounty("Khwisero", listOf("Kisa West", "Kisa East", "Kisa North", "Kisa Central")),
            SubCounty("Shinyalu", listOf("Idakho South", "Idakho East", "Idakho North", "Idakho Central")),
            SubCounty("Navakholo", listOf("Navakholo", "Ingotse/Matungu", "Butali/Chegulo", "Manda/Shivanga")),
            SubCounty("Matungu", listOf("Koyonzo", "Kholera", "Mayoni", "Namamali", "Khalaba")),
            SubCounty("Butere", listOf("South Wanga", "Central Wanga", "Marama East", "Marama Central", "Marama North")),
            SubCounty("Likuyani", listOf("Sango", "Nzoia", "Kongoni", "Likuyani"))
        )),
        // 037 Vihiga
        County("Vihiga", listOf(
            SubCounty("Vihiga", listOf("Lugaga/Wamuluma", "North Maragoli", "Central Maragoli", "Mungoma")),
            SubCounty("Sabatia", listOf("West Sabatia", "Chavakali", "North Maragoli", "Wodanga", "Busali")),
            SubCounty("Hamisi", listOf("Shiru", "Gisambai", "Muhudu", "Shamakaga", "Banja")),
            SubCounty("Luanda", listOf("Emabungo", "North East Bunyore", "Central Bunyore", "West Bunyore")),
            SubCounty("Emuhaya", listOf("North East Bunyore", "Central Bunyore", "West Bunyore", "Emurembe"))
        )),
        // 038 Bungoma
        County("Bungoma", listOf(
            SubCounty("Bumula", listOf("Bumula", "Khasoko", "Kabula", "Kimaeti", "West Nalondo")),
            SubCounty("Kanduyi", listOf("Bukembe West", "Bukembe East", "Township", "Musikoma", "Marakaru/Tuuti")),
            SubCounty("Webuye East", listOf("Mihuu", "Ndivisi", "Maraka")),
            SubCounty("Webuye West", listOf("Matulo", "Chwele", "Sitikho", "Bungoma East")),
            SubCounty("Kimilili", listOf("Kimilili", "Maeni", "Kamukuywa", "Kibingei")),
            SubCounty("Mt Elgon", listOf("Cheptais", "Chesikaki", "Cheptais", "Kapkateny", "Kapsokwony")),
            SubCounty("Tongaren", listOf("Naitiri/Kabuyefwe", "Milima", "Ndalu/Tabani", "Tongaren", "Mbakalo")),
            SubCounty("Sirisia", listOf("Namwela", "Malakisi/South Kulisiru", "Lwandanyi"))
        )),
        // 039 Busia
        County("Busia", listOf(
            SubCounty("Teso North", listOf("Malaba Central", "Ang'urai North", "Ang'urai South", "Malaba North")),
            SubCounty("Teso South", listOf("Amagoro", "Chakol South", "Ang'urai East", "Malaba South")),
            SubCounty("Nambale", listOf("Nambale Township", "Bukhayo North/Walatsi", "Bukhayo East")),
            SubCounty("Matayos", listOf("Butula", "Walatsi", "Lunganyiro", "Busibwabo")),
            SubCounty("Butula", listOf("Bulloha", "Songhor/Soba", "Bwiri")),
            SubCounty("Funyula", listOf("Funyula", "Namboboto Nambuku", "Nangina", "Ageng'a Nanguba")),
            SubCounty("Samia", listOf("Port Victoria", "Sio Port", "Rwambwa", "Muhanda", "Nambuku"))
        )),
        // 040 Siaya
        County("Siaya", listOf(
            SubCounty("Ugenya", listOf("East Ugenya", "Ukwala", "North Ugenya", "West Ugenya")),
            SubCounty("Ugunja", listOf("Sigomere", "Ugunja", "Ukwala")),
            SubCounty("Alego Usonga", listOf("Siaya Township", "Bar Kowino", "Usonga", "West Alego", "Central Alego")),
            SubCounty("Gem", listOf("North Gem", "West Gem", "Central Gem", "Yala Township", "East Gem")),
            SubCounty("Bondo", listOf("Bondo Township", "Usigu", "Yimbo East", "Central Sakwa", "West Sakwa")),
            SubCounty("Rarieda", listOf("East Asembo", "West Asembo", "North Uyoma", "South Uyoma", "Uyoma"))
        )),
        // 041 Kisumu
        County("Kisumu", listOf(
            SubCounty("Kisumu Central", listOf("Railways", "Migosi", "Shaurimoyo Kaloleni", "Market Milimani", "Kondele")),
            SubCounty("Kisumu East", listOf("Kajulu", "Kolwa East", "Manyatta B", "Nyalenda A", "Kolwa Central")),
            SubCounty("Kisumu West", listOf("South West Kisumu", "Central Kisumu", "Kisumu North", "West Seme")),
            SubCounty("Seme", listOf("Central Seme", "East Seme", "West Seme", "North Seme")),
            SubCounty("Nyando", listOf("East Kano/Wawidhi", "Awasi/Onjiko", "Ahero", "Kabonyo/Kanyagwal", "Kobura")),
            SubCounty("Muhoroni", listOf("Miwani", "Ombeyi", "Masogo/Nyang'oma", "Chemelil/Songhor")),
            SubCounty("Nyakach", listOf("South East Nyakach", "North Nyakach", "West Nyakach", "Central Nyakach"))
        )),
        // 042 Homa Bay
        County("Homa Bay", listOf(
            SubCounty("Kasipul", listOf("West Kamagak", "East Kamagak", "Central Kamagak", "Kojwach")),
            SubCounty("Kabondo Kasipul", listOf("Kokwanyo/Kakelo", "Kojwach", "Pala", "Miriu")),
            SubCounty("Karachuonyo", listOf("North Karachuonyo", "Central", "Kanyaluo", "Kibiri")),
            SubCounty("Rangwe", listOf("West Gem", "East Gem", "Kagan", "Kochia")),
            SubCounty("Homa Bay Town", listOf("Homa Bay Central", "Homa Bay Arujo", "Homa Bay East", "Homa Bay West")),
            SubCounty("Ndhiwa", listOf("Kwabwai", "Kanyadoto", "Kanyikela", "Kabuoch North", "Kabuoch South/Pala")),
            SubCounty("Mbita", listOf("Mbita Township", "Kasgunga", "Gembe East", "Gembe West", "Mfangano Island")),
            SubCounty("Suba North", listOf("Gwassi North", "Gwassi South", "Kaksingri West", "Ruma-Kaksingri"))
        )),
        // 043 Migori
        County("Migori", listOf(
            SubCounty("Rongo", listOf("North Kadem", "Minyenya", "Ngege", "Rongo Central", "Mikayi")),
            SubCounty("Awendo", listOf("North Sakwa", "South Sakwa", "West Sakwa", "Central Sakwa")),
            SubCounty("Suna East", listOf("God Jope", "Suna Central", "Kakrao", "Kwa")),
            SubCounty("Suna West", listOf("Wiga", "Wasweta II", "Ragana-Oruba", "Wasimbete")),
            SubCounty("Nyatike", listOf("Kachien'g", "Kanyasa", "North Kadem", "Macalder/Kanyarwanda", "Kachieng'")),
            SubCounty("Uriri", listOf("West Kanyamkago", "North Kanyamkago", "Central Kanyamkago", "East Kanyamkago")),
            SubCounty("Ntimaru", listOf("Ntimaru West", "Ntimaru East", "Nyabasi East", "Nyabasi West")),
            SubCounty("Kuria East", listOf("Gokeharaka/Getambwega", "Ntimaru West", "Ntimaru East")),
            SubCounty("Kuria West", listOf("Masaba South", "Bukira East", "Bukira Central/Ikerege", "Isibania"))
        )),
        // 044 Kisii
        County("Kisii", listOf(
            SubCounty("Bonchari", listOf("Boochi/Tendere", "Bogeka", "Nyamarambe", "Bonchari")),
            SubCounty("South Mugirango", listOf("Boochi/Boochi", "Make", "Gachube")),
            SubCounty("Bomachoge Borabu", listOf("Kiamokama", "Boochi", "Gachube", "Bombaba")),
            SubCounty("Bobasi", listOf("Masige West", "Masige East", "Basi Central", "Nyacheki", "Bobasi Boitangare")),
            SubCounty("Bomachoge Chache", listOf("Township", "Bogusero/Bogeka", "Bomachoge")),
            SubCounty("Nyaribari Masaba", listOf("Iranda", "Gesusu", "Kiogoro", "Itierio")),
            SubCounty("Nyaribari Chache", listOf("Kisii Central", "Bomorenda", "Onchicha", "Monyerero", "Nyanchwa")),
            SubCounty("Kitutu Chache North", listOf("Kegati", "Nyatieko")),
            SubCounty("Kitutu Chache South", listOf("Boikang'a", "Bonyunyu", "Getenga"))
        )),
        // 045 Nyamira
        County("Nyamira", listOf(
            SubCounty("Kitutu Masaba", listOf("Rigoma", "Gachuba", "Kemera", "Magwagwa", "Ekerenyo")),
            SubCounty("West Mugirango", listOf("Nyamira North", "Esise", "Magombo", "Nyamira Township")),
            SubCounty("North Mugirango", listOf("Bomwagamo", "Bokeira", "Magwagwa")),
            SubCounty("Borabu", listOf("Metembe", "Bosamaro", "Bonyamatuta", "Township"))
        )),
        // 046 Nairobi
        County("Nairobi", listOf(
            SubCounty("Westlands", listOf("Kitisuru", "Parklands/Highridge", "Karura", "Kangemi", "Mountain View")),
            SubCounty("Dagoretti North", listOf("Kilimani", "Kawangware", "Gatina", "Kileleshwa", "Lavington")),
            SubCounty("Dagoretti South", listOf("Mutu-ini", "Ngando", "Riruta", "Uthiru/Ruthimitu", "Waithaka")),
            SubCounty("Langata", listOf("Karen", "Nairobi West", "Mugumo-ini", "South C", "Nyayo Highrise")),
            SubCounty("Kibra", listOf("Laini Saba", "Lindi", "Makina", "Woodley/Kenyatta Golf", "Sarang'ombe")),
            SubCounty("Roysambu", listOf("Githurai", "Kahawa West", "Lucky Summer", "Roysambu", "Kahawa")),
            SubCounty("Kasarani", listOf("Clay City", "Mwiki", "Kasarani", "Njiru", "Ruai")),
            SubCounty("Ruaraka", listOf("Babadogo", "Utalii", "Mathare North", "Lucky Summer", "Korogocho")),
            SubCounty("Embakasi South", listOf("Imara Daima", "Kwa Njenga", "Kwa Reuben", "Pipeline", "Mwanja")),
            SubCounty("Embakasi North", listOf("Kariobangi North", "Dandora Area I", "Dandora Area II", "Dandora Area III")),
            SubCounty("Embakasi Central", listOf("Kayole North", "Kayole South", "Kayole Central", "Komarock", "Matopeni/Spring Valley")),
            SubCounty("Embakasi East", listOf("Upper Savanna", "Lower Savanna", "Embakasi", "Utawala", "Mihango")),
            SubCounty("Embakasi West", listOf("Umoja I", "Umoja II", "Mowlem", "Kariobangi South")),
            SubCounty("Makadara", listOf("Maringo/Hamza", "Viwandani", "Harambee", "Makongeni")),
            SubCounty("Kamukunji", listOf("Pumwani", "Eastleigh North", "Eastleigh South", "Airbase", "California")),
            SubCounty("Starehe", listOf("Township", "Pangani", "Ziwani/Kariokor", "Landimawe", "Nairobi Central")),
            SubCounty("Mathare", listOf("Hospital", "Mabatini", "Huruma", "Ngei", "Mlango Kubwa", "Kiamaiko"))
        ))
    )

    fun getCountyNames(): List<String> = counties.map { it.name }.sorted()

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
