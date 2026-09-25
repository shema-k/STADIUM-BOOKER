# Stadium Select — Java Swing Seat Booking

A professional desktop stadium directory and seat-booking application built with Java Swing. The interface guides the user through a connected flow:

**Choose a stadium → choose a date → choose a game or concert → choose seats → confirm booking**

## Features

- Stadium directory with venue information
- Search stadiums by name, city, country, team or artist
- Eleven sample venues with locations, capacity and venue descriptions
- All sample stadiums have more than 20,000 seats
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
- Scalable map rendering for stadiums with 20,000+ seats
- Tapping a seat shows its exact seat number, status and price
- Maximum of six seats per booking
- Booking confirmation with a unique reference
- Searchable booking history
- Booking cancellation
- Local persistence using `stadium-bookings.dat`
- No external libraries or database required

## Included sample data

- **Grand Arena** — New York, 24,000 seats, box-shaped multi-purpose arena
- **Riverside Stadium** — Manchester, 32,000 seats, oval football stadium
- **Pacific Dome** — Los Angeles, 21,600 seats, circular indoor arena
- **Metro Dome** — Chicago, 28,000 seats, circular dome
- **Harbor Arena** — Seattle, 22,400 seats, oval waterfront arena
- **Crown Park** — Toronto, 32,000 seats, box-shaped stadium
- **Southside Coliseum** — Atlanta, 24,800 seats, circular coliseum
- **Desert Field** — Phoenix, 26,400 seats, oval outdoor stadium
- **Nordic Arena** — Stockholm, 21,600 seats, circular winter arena
- **Sakura Stadium** — Tokyo, 36,000 seats, box-shaped national stadium
- **Coastal Arena** — Miami, 30,000 seats, oval coastal arena
- Game schedules include basketball, baseball, football and hockey with named teams
- Concert schedules include named artists and concert titles

## Requirements

- Java Development Kit (JDK) 17 or newer
- A desktop environment with Java Swing support

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
javac -d build/classes $(find src/main/java -name '*.java' -print)
java -cp build/classes com.stadium.booking.StadiumBookingApp
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

Booking data is stored in the project working directory as `stadium-bookings.dat`. Delete that file to reset the demo inventory.
