.global start
start:
    addi    x1, x1, 1
    ori     x2, x1, -3
    add     x3, x1, x2
    la      a0, next
    jr      a0

next:
    slli    x4, x3, 3
    andi    x5, x4, 6

exit:
    .word 0x0000007f # cosim exit instruction.
