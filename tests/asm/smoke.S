.global start
start:
    addi x1, x1, 1

exit:
    # cosim exit instruction.
    .word 0x0000007f
