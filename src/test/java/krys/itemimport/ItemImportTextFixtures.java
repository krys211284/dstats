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
                +217 siły
                +11,0% szansy na trafienie krytyczne
                Przewiń w dół
                """;
    }

    static String stormMoonShieldBottomText() {
        return """
                Przewiń do góry
                17,6% redukcji obrażeń
                12,3% redukcji czasu odnowienia
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
}
