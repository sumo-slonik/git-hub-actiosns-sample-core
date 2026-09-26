# Repozytorium GitHub z CI (GitHub Actions)

To repozytorium pomaga zrozumieć podstawową konfigurację repozytorium na GitHubie, obejmującą:

- **CI** (Continuous Integration) zrealizowane za pomocą GitHub Actions,
- ustawienia polityki dostępu do repozytorium (Rulesets) powiązane z wynikami CI,
- konfigurację CI dla projektu z laboratoriów z Programowania Obiektowego ([obiektowe-lab](https://github.com/Soamid/obiektowe-lab)),
- *(opcjonalnie)* deployment frontendu z wykorzystaniem GitHub Pages.

> Projekt w Javie budujemy Gradle'em. Jeśli chcesz lepiej zrozumieć, czym jest Gradle, jak działają `build.gradle`,
> zależności, taski i Gradle Wrapper (`gradlew`), zajrzyj do repozytorium
> **[gradle-in-java-sample-core](https://github.com/sumo-slonik/gradle-in-java-sample-core)**.

## Po co jest CI?

**Continuous Integration** (ciągła integracja) oznacza, że po każdej zmianie wypchniętej do repozytorium kod jest
**automatycznie budowany i testowany na czystej maszynie**, a nie tylko na komputerze autora. Dzięki temu:

- błąd (np. niekompilujący się kod albo test, który przestał przechodzić) wychodzi od razu, a nie tydzień później,
- znika problem „u mnie działa” - build na serwerze nie korzysta z niczego, czego nie ma w repozytorium,
- przy Pull Requeście od razu widać ✅ lub ❌, więc osoba robiąca code review wie, że kod się buduje i testy przechodzą,
- można zablokować merge do głównego brancha, dopóki testy nie przejdą.

## Czym są GitHub Actions?

GitHub Actions to wbudowany w GitHub system automatyzacji. Możemy je rozumieć jako małe programy, których uruchomienie
jest wywoływane przez konkretne wydarzenia (**eventy**) w naszym repozytorium.

Precyzyjniejszy opis można znaleźć w dokumentacji:
[Understanding GitHub Actions](https://docs.github.com/en/actions/get-started/understand-github-actions) oraz
[Workflow syntax](https://docs.github.com/en/actions/reference/workflows-and-actions/workflow-syntax).

Najważniejsze pojęcia:

- **Event** - coś, co dzieje się w repozytorium: push commita, otwarcie/aktualizacja Pull Requesta, utworzenie issue,
  ręczne kliknięcie „Run workflow” itd.
- **Workflow** - zautomatyzowany proces opisany w pliku `*.yml` w katalogu **`.github/workflows/`**. Eventy trigerują workflow.
- **Job** - workflow składa się z jednego lub więcej jobów. Każdy job uruchamia się na osobnej, świeżej maszynie
  (**runnerze**, np. `ubuntu-latest`). Joby domyślnie wykonują się równolegle.
- **Step** - job składa się z wykonywanych sekwencyjnie kroków. Krok to albo polecenie konsoli (`run:`),
  albo gotowa **akcja** (`uses:`), np. `actions/checkout@v7`, która pobiera kod repozytorium.

Pierwszym krokiem jest wykonanie forka tego repozytorium do swojego profilu.

![Zrzut ekranu z forkiem](readme_images/fork_ss.png)

> W sforkowanym repozytorium GitHub domyślnie wyłącza workflow - wejdź w zakładkę **Actions** i kliknij
> „I understand my workflows, go ahead and enable them”.

W repozytorium znajduje się kod złożony z dwóch części: backendowej (Java + Spring Boot, Gradle) oraz frontendowej
(React, npm). Obie dostarczają podstawowe testy uruchamiane w różny sposób, co pozwoli nam zapoznać się z działaniem GitHub Actions.

## 1. Pierwszy workflow - uruchamianie testów

Workflow, który teraz przygotujemy, będzie uruchamiał testy i będzie składał się z dwóch jobów: jednego dla frontendu, drugiego dla backendu.

1. Utwórz w repozytorium katalog `.github/workflows/`.
2. Skopiuj do niego plik [`resources/tests.yml`](resources/tests.yml) - każdy krok jest w nim opisany komentarzem.
3. Zrób commit i push.

Skrócona wersja najważniejszych elementów:

```yaml
name: Tests

on:
  push:
    branches: [main]
  pull_request:          # testy przy każdym Pull Requeście
  workflow_dispatch:     # ręczne uruchomienie z zakładki Actions

permissions:
  contents: read         # workflow potrzebuje tylko odczytu kodu

jobs:
  backend:
    runs-on: ubuntu-latest
    defaults:
      run:
        working-directory: backend
    steps:
      - uses: actions/checkout@v7
      - uses: actions/setup-java@v6
        with:
          distribution: temurin
          java-version: '17'
      - uses: gradle/actions/setup-gradle@v6
      - run: ./gradlew build
```

> **Uwaga na `gradlew`:** plik `gradlew` musi mieć w repozytorium uprawnienia do wykonywania, inaczej na runnerze
> (Linux) dostaniemy `Permission denied`. Na Windowsie git tego nie ustawia automatycznie - wykonaj raz:
> `git update-index --chmod=+x gradlew`, a potem commit. W tym repozytorium `backend/gradlew` ma już ustawione to uprawnienie.

### Wyniki workflow

Po pushu zmian w zakładce **Actions** powinniśmy zobaczyć uruchomienie naszego workflow:

![Wykonania workflow](readme_images/img.png)

Uruchomienia możemy filtrować po eventach, statusach, branchach itd.

Po kliknięciu w konkretne uruchomienie widzimy jego status i szczegóły:

![Status workflow](readme_images/img_1.png)

W prawym górnym rogu można ponownie uruchomić wszystkie joby (lub tylko te nieudane) bez kolejnego eventu.

Jak widzimy, testy backendu zakończyły się niepowodzeniem. Klikamy joba zakończonego niepowodzeniem, co pozwala zobaczyć wszystkie kroki:

![Kroki workflow](readme_images/img_2.png)

Klikamy czerwony krok i po rozwinięciu widzimy:

![Błąd w kroku](readme_images/img_3.png)

Gdy testy nie przejdą, workflow dodatkowo zapisuje raport HTML z testów jako **artefakt** (`backend-test-report`) -
można go pobrać na dole strony z podsumowaniem uruchomienia.

Test `StudentTest.toStringTest` oczekuje, że `toString()` zawiera wszystkie pola, a pole `id` jest z niego wykluczone.
Naprawiamy to w klasie `backend/src/main/java/pl/agh/slonik/githubactiosns/sample/core/model/Student.java`,
usuwając adnotację `@ToString.Exclude` nad polem `id`, a następnie robimy commit i push.

Po tej zmianie wszystkie testy powinny być zielone ✅

## 2. Polityka dostępu do repozytorium (Rulesets) + wymagane testy

Przy pracy w kilka osób prawdopodobnie nie chcemy, żeby ktokolwiek wypychał commity prosto do głównego brancha bez PR -
ani żeby dało się zmergować PR z czerwonymi testami. Możemy się przed tym zabezpieczyć w zakładce **Settings**:

![Ustawienia repozytorium](readme_images/img_4.png)

Warto zapoznać się z tym, co można tu wyklikać, ale teraz przechodzimy do **Rules → Rulesets**:

![Opcje polityki](readme_images/img_5.png)

Klikamy **New ruleset → New branch ruleset**:

![Nowy zestaw reguł](readme_images/img_6.png)

Ustawiamy nazwę na `main-protection`, a **Enforcement status** na `Active`:

![Ustawienia reguł](readme_images/img_7.png)

Pomijamy część odpowiedzialną za wyjątki (*Bypass list*):

![Wyjątki](readme_images/img_8.png)

Wybieramy branch, dla którego będzie działać polityka (**Add target → Include default branch**):

![Wybór brancha](readme_images/img_9.png)

A następnie reguły:

![Ustawienia reguły](readme_images/img_10.png)

Polecane minimum:

- **Restrict deletions** i **Block force pushes**,
- **Require a pull request before merging** - zmiany do `main` tylko przez PR,
- **Require status checks to pass** → **Add checks** → wybierz joby z naszego workflow (`frontend`, `backend`).
  Dzięki temu przycisk *Merge* w PR będzie zablokowany, dopóki CI nie będzie zielone.
  (Job pojawi się na liście dopiero po tym, jak workflow uruchomi się przynajmniej raz).

Klikamy **Create**.

> Rulesets dla repozytoriów **prywatnych** wymagają planu GitHub Pro/Team - studenci mają Pro za darmo w ramach
> [GitHub Student Developer Pack](https://education.github.com/pack). W repozytoriach publicznych działają na planie darmowym.

## 3. CI dla projektu z laboratoriów z Programowania Obiektowego (obiektowe-lab)

Na laboratoriach z PO ([obiektowe-lab](https://github.com/Soamid/obiektowe-lab)) tworzymy projekt `oolab` typu
**Gradle** (Java 25) i od laboratorium 2 piszemy do niego testy jednostkowe (JUnit). Ten sam mechanizm co powyżej
pozwala automatycznie budować projekt i uruchamiać testy przy każdym pushu oraz w każdym Pull Requeście
z rozwiązaniem laboratorium.

Gotowy plik workflow: [`resources/oolab-ci.yml`](resources/oolab-ci.yml). Konfiguracja jest sprawdzona na projekcie
z laboratorium 2 (Java 25, Gradle 9.8, JUnit 5).

### Co trzeba skonfigurować w projekcie

1. **Umieść workflow w swoim repozytorium** jako `.github/workflows/ci.yml`. Katalog `.github` musi leżeć
   w **głównym katalogu repozytorium** (tam, gdzie `.git`), a nie w katalogu projektu `oolab`.

2. **Ustaw katalog projektu** w `working-directory`. Workflow zakłada, że projekt Gradle leży w katalogu `oolab/`
   w repozytorium (obok np. `README.md`). Jeśli `build.gradle` i `gradlew` są bezpośrednio w głównym katalogu
   repozytorium, ustaw `working-directory: .` i zmień ścieżkę raportu na `build/reports/tests/test`.

3. **Wrzuć do repozytorium Gradle Wrapper.** CI buduje projekt poleceniem `./gradlew`, więc w repozytorium muszą być:
   `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`, `gradle/wrapper/gradle-wrapper.properties`
   oraz `build.gradle` i `settings.gradle`. Sprawdź, czy `.gitignore` ich nie wyklucza
   (katalogi `.gradle/` i `build/` natomiast **powinny** być ignorowane).

4. **Nadaj `gradlew` prawo do wykonywania** (szczególnie jeśli pracujesz na Windowsie):
   ```bash
   git update-index --chmod=+x oolab/gradlew
   git commit -m "Make gradlew executable"
   ```

5. **Ta sama wersja Javy lokalnie i w CI.** W `build.gradle` (lab 1) ustawiamy toolchain:
   ```groovy
   java {
       toolchain {
           languageVersion.set(JavaLanguageVersion.of(25))
       }
   }
   ```
   W workflow `java-version` w `actions/setup-java` powinno mieć tę samą wartość (`'25'`).

   Java 25 wymaga **Gradle w wersji 9.1 lub nowszej**. Wersję sprawdzisz w `gradle/wrapper/gradle-wrapper.properties`
   (linia `distributionUrl=...gradle-9.x-bin.zip`). Jeśli jest starsza, zaktualizuj wrapper poleceniem
   `./gradlew wrapper --gradle-version latest` i zacommituj zmienione pliki wrappera.

6. **Testy muszą być uruchamiane przez Gradle.** Projekt wygenerowany przez IntelliJ ma już odpowiednią konfigurację -
   upewnij się, że w `build.gradle` są (wersja `junit-bom` może być inna):
   ```groovy
   dependencies {
       testImplementation platform('org.junit:junit-bom:5.10.0')
       testImplementation 'org.junit.jupiter:junit-jupiter'
       testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
   }

   test {
       useJUnitPlatform()
   }
   ```
   Linia `testRuntimeOnly 'org.junit.platform:junit-platform-launcher'` jest w Gradle 9 **obowiązkowa** - bez niej
   uruchamianie testów kończy się błędem `Failed to load JUnit Platform`.
   Testy trzymamy w `src/test/java` - tylko stamtąd Gradle je uruchomi.

7. **Sprawdź lokalnie, zanim wypchniesz:** `./gradlew build` (lub `gradlew.bat build` na Windowsie) w katalogu projektu.
   Jeśli lokalnie przechodzi, w CI też powinno.

8. **Actions w repozytorium prywatnym.** Repozytoria z rozwiązaniami są prywatne - GitHub Actions działa w nich,
   ale w ramach limitu minut (2000 min/mies. na darmowym planie, 3000 na Pro) - to z dużym zapasem wystarcza.
   Upewnij się, że w *Settings → Actions → General* akcje są dozwolone (*Allow all actions and reusable workflows*).
   W workflow używamy `cache-provider: basic` w `setup-gradle`, który działa bez dodatkowych warunków także w repozytoriach prywatnych.

9. **(Opcjonalnie)** dodaj ruleset z punktu 2 dla brancha `main` i jako wymagany check wybierz job `build` -
   PR z rozwiązaniem laboratorium nie da się zmergować z czerwonymi testami.

Po pushu na branch (np. `lab2`) w zakładce **Actions** zobaczysz uruchomienie workflow `CI`, a w Pull Requeście
`lab2 → main` - status ✅/❌ przy commitach i w sekcji *Checks*.

## 4. *(Opcjonalnie)* Deployment frontendu z wykorzystaniem GitHub Pages

> Ta część **nie jest potrzebna** do CI ani do laboratoriów z PO - dotyczy tylko strony React z katalogu `frontend`.

Proste statyczne strony można za darmo hostować na GitHub Pages. Nasz frontend nie komunikuje się z backendem,
więc możemy z tego skorzystać. Dokumentacja:
[Using custom workflows with GitHub Pages](https://docs.github.com/en/pages/getting-started-with-github-pages/using-custom-workflows-with-github-pages).

Obecnie nie potrzebujemy do tego ani osobnego brancha `gh-pages`, ani własnego Personal Access Tokena:

1. W repozytorium wejdź w **Settings → Pages** i w sekcji **Build and deployment → Source** wybierz **GitHub Actions**.
2. Skopiuj [`resources/deploy.yml`](resources/deploy.yml) do `.github/workflows/`.
3. Zrób push do `main` (lub uruchom workflow ręcznie z zakładki Actions).

Workflow buduje stronę (`npm run build`), pakuje katalog `frontend/build` akcją `actions/upload-pages-artifact`
i publikuje go akcją `actions/deploy-pages`, korzystając z wbudowanego `GITHUB_TOKEN` (uprawnienia `pages: write`
i `id-token: write`). Adres strony (`https://<github-username>.github.io/<repo-name>/`) pojawi się w podsumowaniu
uruchomienia i w *Settings → Pages*; ścieżka jest przekazywana do builda automatycznie (`PUBLIC_URL`),
więc nie trzeba dopisywać `homepage` w `package.json`.

![Deploy workflow](readme_images/img_14.png)
