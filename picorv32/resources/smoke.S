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
    la      a0, exit
    lw      a0, 0(a0)
    la      a1, data
    sw      a0, 0(a1)
    bne     x3, x4, 1f
    j       next
1:  j       data

exit:
    .word 0x0000007f # cosim exit instruction.

data:
    .word 0
