package krys.itemimport;

/** Wspólne fixture tekstowe importu itemów. */
final class ItemImportTextFixtures {
    private ItemImportTextFixtures() {
    }

    static String realShieldTopText() {
        return """
                Miażdżąca Tarcza Kościanych Łusek
                Starożytna legendarna tarcza
                Moc przedmiotu: 900
                25 (+25) jakości
                Przeistoczony
                1 502 pkt. pancerza
                (Wytrzymałość: +4,6%)
                20,0% szansy na blok [20,0]%
                +100% obrażeń od broni w głównej ręce [100]%
                +270 siły
                +588 do odporności na wszystkie żywioły
                +945 do odporności na: Ogień
                Przewiń w dół
                Wyposaż
                Porównaj
                """;
    }

    static String realShieldBottomText() {
        return """
                Przewiń do góry
                +945 do odporności na: Ogień
                14,3% redukcji obrażeń [11,0 - 15,0]%
                +96 pkt. do wszystkich współczynników [+75 - 100]
                +12 do maksymalnej liczby kumulacji Animuszu
                Gdy masz umocnienie, zadajesz obrażenia zwiększone o 61%[x] [45 - 65]%.
                Puste gniazdo
                Przedmiot z dodatku Lord of Hatred
                Brak możliwości modyfikacji
                Wartość sprzedaży: 38 450
                Trwałość: 100/100
                Oznacz jako śmieć
                Upuść
                """;
    }

    static String stormMoonShieldTopText() {
        return """
                Tarcza Burzy Księżycowego Szału
                Starożytna legendarna tarcza
                Moc przedmiotu: 900
                29 (+25) jakości
                Przeistoczony
                1 502 pkt. pancerza
                Rynsztunek w Zbrojowni
                20,0% szansy na blok [20,0]%
                +100% obrażeń od broni w głównej ręce [100]%
                +217 siły [150 - 180]
                +11,0% szansy na trafienie krytyczne [6,5 - 8,5]%
                Przewiń w dół
                """;
    }

    static String stormMoonShieldBottomText() {
        return """
                Przewiń do góry
                17,6% redukcji obrażeń [11,0 - 15,0]%
                12,3% redukcji czasu odnowienia [10 - 12]%
                +4 do jakości przedmiotu [1 - 15]
                +12 do maksymalnej liczby kumulacji Animuszu
                Puste gniazdo
                Naznaczenie
                Zadanie wrogowi obrażeń umiejętnością Podstawową zwiększa twoją szybkość ataku o 4% na 10 sek. Efekt kumuluje się maksymalnie 5 razy. Przy maksymalnej kumulacji wchodzisz w stan Wampirycznego Szału Krwi, który zapewnia zwiększenie obrażeń od umiejętności Podstawowych o 60%[x] oraz zwiększenie szybkości ruchu o 15% przez 10 sek.
                Brak możliwości modyfikacji
                Wymaga 70 poziomu
                Przypisano do konta
                Wartość sprzedaży: 38 450
                Trwałość: 100/100
                """;
    }

    static String verathielCondensedTextWithDamagedRollRanges() {
        return "Odłamek Verathiela Starożytny unikatowy miecz Moc przedmiotu: 900 "
                + "1 874 pkt. obrażeń na sek. [1 390 - 2 018] pkt. obrażeń za trafienie "
                + "1,10 ataku na sekundę +134 obrażeń od broni [94 - 1571 "
                + "+172 siły [150 - 1801 +300 zdrowia za zabicie [+300] "
                + "Mnożnik x16% obrażeń z upływem czasu [15 - 301% "
                + "Umiejętności Podstawowe zadają obrażenia zwiększone o 100%[x] [70 - 100]%, "
                + "ale dodatkowo zużywają 25 pkt. podstawowego zasobu.";
    }

    static String heirOfPerditionTopText() {
        return """
                DZIEDZIC
                ZATRACENIA
                Starożytny mityczny
                unikatowy hełm
                Moc przedmiotu: 900
                25 (* +25) jakości
                Rynsztunek w Zbrojowni
                Przeistoczony
                2 004 pkt. pancerza
                (Wytrzymałość: +32,8%)
                +15,0% szansy na trafienie
                krytyczne [12,0]%
                +25,0% szansy na szczęśliwy traf
                [20,0]%
                +25% szybkości ruchu [20]%
                +3 do umiejętności: Główne [3]
                +115 pkt. do wszystkich
                współczynników +[75 - 100]
                +12 do maksymalnej liczby
                kumulacji Animuszu
                Poddaj się nienawiści i doświadcz
                Łaski Matki, która zwiększy
                zadawane przez ciebie obrażenia o
                80%[x]. Zabijaj wrogów, aby na
                chwilę ukraść pobliskim
                sojusznikom efekt Łaski Matki.
                Puste gniazdo
                """;
    }

    static String heirOfPerditionBottomText() {
        return """
                Przewiń do góry
                +25,0% szansy na szczęśliwy traf
                [20,0]%
                +25% szybkości ruchu [20]%
                +3 do umiejętności: Główne [3]
                +115 pkt. do wszystkich
                współczynników +[75 - 100]
                +12 do maksymalnej liczby
                kumulacji Animuszu
                Poddaj się nienawiści i doświadcz
                Łaski Matki, która zwiększy
                zadawane przez ciebie obrażenia o
                80%[x]. Zabijaj wrogów, aby na
                chwilę ukraść pobliskim
                sojusznikom efekt Łaski Matki.
                Puste gniazdo
                Puste gniazdo
                Wymaga 70 poziomu
                Przypisano do konta
                Unikatowe wyposażenie
                Przedmiot z dodatku Lord of Hatred
                Brak możliwości modyfikacji
                Wartość sprzedaży: 115 350
                Trwałość: 70/100
                """;
    }

    static String heirOfPerditionBottomTextWithSocketGemStats() {
        return heirOfPerditionBottomText()
                .replace("Puste gniazdo\nPuste gniazdo",
                        """
                        +150 siły
                        +120 siły
                        +120 inteligencji
                        +500 pkt. pancerza
                        Puste gniazdo
                        Puste gniazdo""");
    }

    static String heirOfPerditionBottomTextWithJoinedSocketGemStatsAndFooter() {
        return """
                Przewiń do góry
                +25,0% szansy na szczęśliwy traf
                [20,0]%
                +25% szybkości ruchu [20]%
                +3 do umiejętności: Główne [3]
                +115 pkt. do wszystkich
                współczynników +[75 - 100]
                +12 do maksymalnej liczby
                kumulacji Animuszu
                Poddaj się nienawiści i doświadcz Łaski Matki, która zwiększy zadawane przez ciebie obrażenia o 80%[x]. Zabijaj wrogów, aby na chwilę ukraść pobliskim sojusznikom efekt Łaski Matki. Ę +150 siły
                . SIŁY +120 siły +120 inteligencji +120 zręczności +120 siły woli +500 pkt. pancerza Puste gniazdo Wymaga 70 poziomu Przypisano do konta
                Unikatowe wyposażenie
                Przedmiot z dodatku Lord of Hatred
                Brak możliwości modyfikacji
                Wartość sprzedaży: 115 350
                Trwałość: 70/100
                """;
    }

    static String heirOfPerditionCurrentScreenTopTextWithCoreRanks2() {
        return heirOfPerditionTopText()
                .replace("+3 do umiejętności: Główne [3]", "+2 do umiejętności: Główne [2]");
    }

    static String heirOfPerditionCurrentScreenBottomTextWithCoreRanks2() {
        return heirOfPerditionBottomText()
                .replace("+3 do umiejętności: Główne [3]", "+2 do umiejętności: Główne [2]");
    }
}
