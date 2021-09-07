# Notes

## Puzzle in the ruins

    _ + _ * _^2 + _^3 - _ = 399

Have to place the coins with the correct values in the slots in order?

- red coin: 2
- corroded coin: 3
- shiny coin: 5
- concave coin: 7
- blue coin: 9

so 9,2,5,7,3 or blue, red, shiny, concave, corroded.

## Hacking the teleporter

The check is at address 5489. There's a call to 6027, which leaves some result
in r0. If this result is 6, we successfully teleport to the beach. We can
bypass the check and jump straight after the call and jf with

    #(update % :memory assoc 5489 6 5490 5498)

but we still need to figure out what value of r7 will return a 6 from 6027.

Decompiling the code, writing it out in clojure and gradually transforming it
to be highter level... it turns out to be Ackerman's function! Testing each
input in turn eventually gives us 25734 as a value of r7 which will result in
a check of 6.

    #(update % :registers assoc 7 25734)

## Orb puzzle

After messing with the VM in the last puzzle, my first thought was to hack this
one as well. Changing a jt to a jf after the eq r0 30 check seems to do the
trick...

    #(update % :memory assoc 4557 8)

But then the code you get is wrong, so it must depend on the state in some way
that I'm violating. Time to solve it properly I guess.

The floor plan is

    *   8   -   1
    4   *  11   *
    +   4   -  18
    22  -   9   *

with the start at 22, and the vault at 1. We need the orb to weigh 30 when we
reach the vault.

## Codes

- `SpKPqHYzYxUp` given in the hints in arch-spec
- `noEYBZZSyezn` VM prints this after implementing 19 and 21
- `lxJymKTjTAso` VM prints this after completing self-test
- `FfJqweDRnEuo` obtained by writing on the tablet
- `QvZJEumZbLqY` chiseled on the wall in the Twisty passages
- `EfGriRahcFsv` written in the stars when teleporting
- `nWMQDVBYuKdT` found after hacking the teleporter
