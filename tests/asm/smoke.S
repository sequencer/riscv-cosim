.global test
test:
    addi x1, x1, 1

exit:
    # Magic instruction to end simulation.
    csrwi 0x7cc, 0
