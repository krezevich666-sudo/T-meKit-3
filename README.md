# TāmeKit – 2.3 iterācija (gatava demonstrācijai)

Kas jauns salīdzinājumā ar 2.2:
- Kustību logā ienākšanas / izlietojuma / korekcijas pogas ir vienāda platuma vienā rindā, un logs ir platāks, lai "Korekcija" neplīstu.
- Materiāliem ir meklēšanas lauks (pēc nosaukuma un kategorijas).
- Iestatījumos sadaļa "Demonstrācija": "Ielādēt demonstrācijas datus" (5 auto krāsu produkti, viens klients, viens izlietojums,
  viens produkts zem minimālā atlikuma) un "Dzēst visus datus" (ar brīdinājumu). Demo dati tiek ielādēti TIKAI tad, ja datubāze ir tukša.
- Jauni testi demo datiem (Robolectric).

Ieteicamā demonstrācijas kārtība:
1. Iestatījumi -> Dzēst visus datus (ja iepriekš bija testa dati; pirms tam vari eksportēt rezerves kopiju).
2. Iestatījumi -> Ielādēt demonstrācijas datus.
3. Materiāli: parādi sarakstu un meklēšanu (sarkanais teksts = zem minimālā atlikuma).
4. Pieskaries produktam: pievieno piegādi, izsniedz materiālu, parādi vēsturi.
5. Iestatījumi -> Eksportēt datus.
Svarīgi: pirms reālu uzņēmuma datu ievades šajā versijā vēl nav migrāciju, tāpēc to nedod lietot uzņēmumam darbā.

Apzināti NAV iekļauts (atkarīgs no sarunu rezultātiem): mērvienību/iepakojumu pārstrukturēšana, sākuma ekrāna panelis, kategoriju filtri, CSV/PDF eksports, svītrkodi.
Būvēšana: tāpat kā iepriekš (GitHub Actions). Ja testi vai būvēšana kļūdās, atsūti kļūdas tekstu.
