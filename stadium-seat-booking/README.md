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
- Booking confirmation with a unique reference
- Searchable booking history
- Click any booking row to open complete booking and customer details
- Back buttons in the main header and opened dialogs for returning to the previous window
- Hover, pressed and released highlighting on every button so the targeted control is always obvious
- Status bar and tooltip name the button under the pointer and the button being clicked
- Booking cancellation
- Persistent H2 database for bookings and customer details
- Bookings section reads complete records from the database
- No external database server required; the embedded database is bundled in `lib/`

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

## How to use it

1. Search for a stadium or choose one from the directory cards.
2. Review the stadium information, upcoming schedule, countdown and special notices.
3. Use the date selector and event search bar to find a game or concert.
4. Open the event to view its sport, teams or artist, date, start time, doors time and booking deadline.
5. Choose section A, B, C or D and tap a vacant seat. The map shows the exact seat number and price.
6. Review the price outline: each selected seat, row tier, subtotal, booking fee and total due.
7. Press **Confirm booked seats** to complete the reservation.
8. Review the booked seat numbers in the confirmation message.
9. Open **My bookings** to search, review or cancel a reservation.

Bookings are stored in the embedded H2 database `stadium-bookings.mv.db` in the project working directory. Delete that file to reset the database. Older `stadium-bookings.dat` files are migrated automatically when found.
