# Stadium Select — Java Swing Seat Booking

A professional desktop stadium directory and seat-booking application built with Java Swing. The interface guides the user through a connected flow:

**Choose a stadium → choose a date → choose a game or concert → choose seats → confirm booking**

## Features

- Stadium directory with venue information
- Search stadiums by name, city, country, team or artist
- Eleven real Ugandan venues with locations, capacity and venue descriptions
- Real published capacities, from Namboole's 45,202 down to Pece Stadium's 3,000
- Box-shaped, oval and circular stadium layouts
- Four seating ends/sides (A, B, C and D) with independent rows
- Front rows use premium pricing; middle and back rows progressively decrease in price
- Date-based event schedules for every stadium
- **Live schedules** view listing every upcoming game and concert across all venues, soonest first
- Live schedule filters by date, type (games or concerts) and free-text search
- Cancelled and emergency-affected events are blocked and labelled in the live schedule
- Search events by team, artist, sport, date or time
- Game events include the sport and both teams
- Concert events include the artist and doors time
- Event-specific seat pricing and availability
- Price outline showing every selected seat, row tier, subtotal, booking fee and total due
- Explicit **Confirm booked seats** button
- Live vacancy percentage and remaining-seat count
- Booking deadline shown for every event
- Live countdown showing days, months, hours or minutes remaining
- Special notices and requests section for cancellations, emergencies and venue announcements
- Ability to submit a special request to the stadium team
- Simplified booking form with only name, email and phone
- Interactive seat map for sections A, B, C and D
- Scalable map rendering, from 3,000-seat grounds up to Namboole's 45,202
- Tapping a seat shows its exact seat number, status and price
- No limit on the number of bookings per person; each reservation can contain up to six seats
- Each reservation is spread across the four sections, so six seats land two in each of two
  sections and four land one in each of four
- Five-minute seat holds with a live countdown, so two people cannot pick the same seat and
  only discover it at confirmation
- Held seats show their own colour on the map and a second customer is turned away at once
- Booking confirmation with a unique reference
- Searchable booking history
- Click any booking row to open complete booking and customer details
- **Booked seats** section listing every seat taken so far, per stadium and per event
- Occupancy summary with seats booked, total capacity, vacancy and a per-section breakdown
- Booked seats view opens pre-set to the event you are booking, via **View booked seats** on the seat screen
- Back buttons in the main header and opened dialogs for returning to the previous window
- Hover, pressed and released highlighting on every button so the targeted control is always obvious
- Status bar and tooltip name the button under the pointer and the button being clicked
- Booking cancellation
- Payment step before the ticket is issued: cash at the venue, or mobile money
- Printable e-ticket with the reference, event, venue, seats, fees and total
- Ticket check code derived from the reference and seats, so a paper ticket can be
  matched back to the database by eye
- Ticket can be saved as text or sent to a printer
- Export bookings to CSV, and the occupancy report to its own CSV
- **Occupancy** report across every venue: events, seats on sale, seats booked, vacancy
  and a per-section breakdown, with rows that sum to the totals
- Keyboard seat selection: the arrow keys walk the grid and Enter or Space holds a seat,
  so booking works without a mouse
- Accessible names and descriptions on the search fields, customer fields and navigation
- Interface available in English, Luganda and Swahili, switchable from the header
- Staff PIN gates the screens that show customer contact details
- Customer names are shown partially and email and phone are withheld until someone
  signs in, so contact details are not exposed to whoever opens the application
- The PIN is stored only as a salted hash, and five wrong attempts lock the session
- Persistent H2 database for bookings and customer details
- Bookings section reads complete records from the database
- Each booking is written to the database in its own transaction, and seats are held in a
  table with a primary key of event plus seat position, so the database itself refuses to
  sell the same seat twice
- Booking references are allocated by reading the database, so two people booking at the
  same time never collide
- No external database server required; the embedded database is bundled in `lib/`

## Tests

```bash
./run-tests.sh
```

Seventy-two tests covering the venue and event data, pricing and booking rules, database
persistence, the section spread, seat holds, the staff PIN, tickets and export, payments,
the three languages and the occupancy report. The runner needs nothing but a JDK, so no
build tool or network access is required. Two of them are regression tests for the defect
where a confirmed booking could be silently destroyed when two people saved at once.

## Included sample data

Real Ugandan venues, clubs and artists. Capacities are the published figures, and each
venue's seat grid totals exactly its stated capacity.

| Venue | City | Capacity | Home club / use |
|---|---|---|---|
| Mandela National Stadium (Namboole) | Kampala | 45,202 | Uganda Cranes, URA FC, Police FC |
| Hoima City Stadium | Hoima | 20,000 | Kitara FC |
| Hamz Stadium (Nakivubo) | Kampala | 15,000 | Express FC |
| St. Mary's Stadium, Kitende | Entebbe | 15,000 | Vipers SC |
| Kyabazinga Stadium, Bugembe | Jinja | 12,000 | Jinja North United FC |
| MTN Omondi Stadium, Lugogo | Kampala | 10,000 | KCCA FC |
| Mutesa II Stadium, Wankulukuku | Kampala | 8,000 | Kampala city stadium |
| FUFA Kadiba Stadium | Kampala | 7,000 | SC Villa |
| Bunamwaya Stadium | Wakiso Town | 5,000 | Community club ground |
| Mbale Municipal Stadium | Mbale | 5,000 | Eastern Uganda municipal ground |
| Pece War Memorial Stadium | Gulu | 3,000 | Gulu United FC |

- Game schedules use real Uganda Premier League clubs — Vipers SC, SC Villa, KCCA FC,
  Express FC, URA FC, Police FC, NEC FC, Kitara FC, Maroons FC, Mbarara City FC,
  Lugazi FC, UPDF FC, Blacks Power FC, Kataka FC, Kigezi Homeboyz FC, BUL FC,
  Gaddafi FC, Booma FC and more — plus the Uganda Cranes against regional national sides
- Concerts use real Ugandan artists — Eddy Kenzo, Bobi Wine, Jose Chameleone, Bebe Cool,
  Fik Fameica, Azawi, Spice Diana, King Saha, Radio & Weasel, Sheebah, John Blaq,
  Juliana Kanyomozi and Iryn Namubiru
- Football, netball and rugby fixtures, priced in Ugandan shillings (UGX) and rounded
  to the nearest 500 shillings, with a UGX 15,000 ticketing fee per reservation
- Seating sections A to D are the VIP Box, Main Stand, Terrace and Kampala End, with
  front rows priced highest and the Kampala End cheapest

## Requirements

- Java Development Kit (JDK) 17 or newer
- A desktop environment with Java Swing support
- The bundled H2 JDBC driver in `lib/` (included with the project)

## Run on Linux/macOS

```bash
cd /home/shema/Desktop/PROJECTS/stadium-seat-booking
./run.sh
```

The script compiles the project into `build/classes` and opens the GUI.

## Run manually

From the project directory:

```bash
mkdir -p build/classes
javac -cp 'lib/*' -d build/classes $(find src/main/java -name '*.java' -print)
java -cp 'build/classes:lib/*' com.stadium.booking.StadiumBookingApp
```

## Build with Maven

A `pom.xml` is included for environments that prefer Maven. It packages the application and
copies the H2 driver next to the jar:

```bash
mvn -q package
java -jar target/stadium-select.jar
```

## How to use it

1. Search for a stadium or choose one from the directory cards.
2. Review the stadium information, upcoming schedule, countdown and special notices.
3. Use the date selector and event search bar to find a game or concert.
4. Open the event to view its sport, teams or artist, date, start time, doors time and booking deadline.
5. Choose section A, B, C or D and tap a vacant seat. The map shows the exact seat number and price.
6. Review the price outline: each selected seat, row tier, subtotal, booking fee and total due.
7. Press **Confirm booked seats** to complete the reservation.
8. Review the booked seat numbers in the confirmation message.
9. Choose how to pay. Cash at the venue is the default.
10. Save or print the ticket that is issued.
11. Open **My bookings** to search, review or cancel a reservation. This asks for the staff
    PIN; the default is `1234`.

The **Occupancy** button in the header shows how full every venue is. The **Booked seats**
button lists every seat already taken, by venue and event.

## Notes

Bookings are stored in the embedded H2 database `stadium-bookings.mv.db` in the project
working directory. Delete that file to reset the database. Older `stadium-bookings.dat`
files are migrated automatically when found.

**Mobile money is simulated.** The payment step records a mobile money authorisation
locally and labels it as such; no money moves and no provider credentials ship with this
build. A real transfer needs a merchant account with a payment provider, wired in at
`PaymentRecord`. Cash at the venue needs no third party and is the default.

**Two capacities are indicative, not published.** Kyabazinga Stadium (12,000) and Mbale
Municipal Stadium (5,000) are real FUFA venues, but their capacities have not been
officially published. Every other capacity is the published figure.
