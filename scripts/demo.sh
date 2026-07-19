#!/usr/bin/env bash
#
# End-to-end demo of the Movie Ticket Booking System.
# Prereq: the app is running (./gradlew bootRun) and local Postgres is up.
#
# Usage:  ./scripts/demo.sh            # against http://localhost:8080
#         BASE_URL=http://host:port ./scripts/demo.sh
#
set -euo pipefail

BASE="${BASE_URL:-http://localhost:8080}/api/v1"
CT="Content-Type: application/json"
ADMIN_EMAIL="admin@moviebooking.com"
ADMIN_PASS="Admin@12345"

# --- helpers ---------------------------------------------------------------
jq_get() { python3 -c "import sys,json;print(json.load(sys.stdin)$1)"; }
step()   { printf "\n\033[1;36m== %s ==\033[0m\n" "$1"; }
info()   { printf "   %s\n" "$1"; }

req() { # METHOD PATH [TOKEN] [BODY]
  local method="$1" path="$2" token="${3:-}" body="${4:-}"
  local args=(-s -X "$method" "$BASE$path" -H "$CT")
  [[ -n "$token" ]] && args+=(-H "Authorization: Bearer $token")
  [[ -n "$body"  ]] && args+=(-d "$body")
  curl "${args[@]}"
}

# --- 0. sanity -------------------------------------------------------------
step "0. Health check"
curl -sf "${BASE_URL:-http://localhost:8080}/actuator/health" >/dev/null \
  && info "app is UP" \
  || { echo "App not reachable — start it with ./gradlew bootRun"; exit 1; }

# --- 1. admin sets up the catalog -----------------------------------------
step "1. Admin logs in and builds the catalog"
SFX=$(date +%s)
ADMIN=$(req POST /auth/login "" "{\"email\":\"$ADMIN_EMAIL\",\"password\":\"$ADMIN_PASS\"}" | jq_get "['accessToken']")
info "admin token acquired"

CITY=$(req POST /admin/cities "$ADMIN" "{\"name\":\"Demo City $SFX\",\"state\":\"Karnataka\"}" | jq_get "['id']")
THEATRE=$(req POST /admin/theatres "$ADMIN" "{\"cityId\":$CITY,\"name\":\"Demo IMAX $SFX\",\"address\":\"MG Road\"}" | jq_get "['id']")
SCREEN=$(req POST /admin/screens "$ADMIN" "{\"theatreId\":$THEATRE,\"name\":\"Screen 1\"}" | jq_get "['id']")
req POST "/admin/screens/$SCREEN/seats" "$ADMIN" \
  '{"rows":[{"rowLabel":"A","seatType":"RECLINER","count":4},{"rowLabel":"B","seatType":"REGULAR","count":6}]}' >/dev/null
MOVIE=$(req POST /admin/movies "$ADMIN" '{"title":"Inception","language":"English","genre":"SciFi","durationMin":148,"certificate":"UA","releaseDate":"2010-07-16"}' | jq_get "['id']")
POLICY=$(req POST /admin/refund-policies "$ADMIN" '{"name":"Standard '"$SFX"'","rules":[{"minMinutesBeforeShow":1440,"refundPercent":100},{"minMinutesBeforeShow":120,"refundPercent":50},{"minMinutesBeforeShow":30,"refundPercent":0}]}' | jq_get "['id']")
req POST /admin/discounts "$ADMIN" '{"code":"DEMO10-'"$SFX"'","discountType":"PERCENT","value":10,"maxDiscountAmount":10000,"minOrderAmount":0,"validFrom":"2026-01-01T00:00:00Z","validTo":"2030-01-01T00:00:00Z"}' >/dev/null
info "city=$CITY theatre=$THEATRE screen=$SCREEN movie=$MOVIE policy=$POLICY"

START=$(date -u -v+2d '+%Y-%m-%dT%H:00:00Z' 2>/dev/null || date -u -d '+2 days' '+%Y-%m-%dT%H:00:00Z')
SHOW=$(req POST /admin/shows "$ADMIN" "{\"movieId\":$MOVIE,\"screenId\":$SCREEN,\"startTime\":\"$START\",\"refundPolicyId\":$POLICY,\"prices\":[{\"seatType\":\"RECLINER\",\"price\":45000},{\"seatType\":\"REGULAR\",\"price\":20000}]}" | jq_get "['id']")
info "show=$SHOW starts $START"

# --- 2. public browse ------------------------------------------------------
step "2. Customer browses (public, no auth)"
info "shows in city $CITY:"
req GET "/shows?cityId=$CITY" | jq_get "[0]['shows'][0]" | sed 's/^/   /'
SEATS=$(req GET "/shows/$SHOW/seats")
SEAT_A1=$(echo "$SEATS" | jq_get "['seats'][0]['seatId']")
SEAT_A2=$(echo "$SEATS" | jq_get "['seats'][1]['seatId']")
info "picking seats: $SEAT_A1, $SEAT_A2"

# --- 3. register + hold ----------------------------------------------------
step "3. Customer registers, logs in, holds seats"
CUST_EMAIL="demo-$SFX@example.com"
req POST /auth/register "" "{\"name\":\"Demo User\",\"email\":\"$CUST_EMAIL\",\"password\":\"Secret123\"}" >/dev/null
CUST=$(req POST /auth/login "" "{\"email\":\"$CUST_EMAIL\",\"password\":\"Secret123\"}" | jq_get "['accessToken']")
HOLD=$(req POST /holds "$CUST" "{\"showId\":$SHOW,\"seatIds\":[$SEAT_A1,$SEAT_A2]}")
HOLD_ID=$(echo "$HOLD" | jq_get "['holdId']")
info "hold=$HOLD_ID  total=$(echo "$HOLD" | jq_get "['totalAmount']") paise  expires in $(echo "$HOLD" | jq_get "['secondsRemaining']")s"

# --- 4. checkout with discount --------------------------------------------
step "4. Checkout preview with a discount code"
CO=$(req GET "/holds/$HOLD_ID/checkout?discountCode=DEMO10-$SFX" "$CUST")
info "total=$(echo "$CO" | jq_get "['totalAmount']")  discount=$(echo "$CO" | jq_get "['discountAmount']")  payable=$(echo "$CO" | jq_get "['payableAmount']") paise"

# --- 5. book + pay ---------------------------------------------------------
step "5. Create booking, then pay (simulated)"
BOOKING=$(req POST /bookings "$CUST" "{\"holdId\":$HOLD_ID,\"discountCode\":\"DEMO10-$SFX\"}")
BOOKING_ID=$(echo "$BOOKING" | jq_get "['id']")
info "booking=$(echo "$BOOKING" | jq_get "['bookingRef']")  status=$(echo "$BOOKING" | jq_get "['status']")  payable=$(echo "$BOOKING" | jq_get "['payableAmount']")"
PAY=$(req POST /payments "$CUST" "{\"bookingId\":$BOOKING_ID,\"method\":\"UPI\",\"idempotencyKey\":\"demo-$SFX\"}")
info "payment status=$(echo "$PAY" | jq_get "['status']")  txn=$(echo "$PAY" | jq_get "['txnRef']")"
info "booking is now: $(req GET "/bookings/$BOOKING_ID" "$CUST" | jq_get "['status']")"
info "seat map now:   $(req GET "/shows/$SHOW/seats" | jq_get "['seats'][0]['status']") (A1)"

# --- 6. cancel + refund ----------------------------------------------------
step "6. Cancel the confirmed booking (triggers async refund + notifications)"
req POST "/bookings/$BOOKING_ID/cancel" "$CUST" '{"reason":"demo"}' | jq_get "['status']" | sed 's/^/   status=/'
info "polling for the async refund..."
for i in $(seq 1 20); do
  R=$(req GET "/refunds?bookingId=$BOOKING_ID" "$CUST" || true)
  if echo "$R" | grep -q PROCESSED; then
    info "refund: amount=$(echo "$R" | jq_get "['amount']") paise  status=$(echo "$R" | jq_get "['status']")"
    break
  fi
  sleep 0.5
done
info "seat map now:   $(req GET "/shows/$SHOW/seats" | jq_get "['seats'][0]['status']") (A1 released)"

step "Done"
info "Check the app logs for the [EMAIL]/[PUSH] notification lines."
