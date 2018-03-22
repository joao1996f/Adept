	.file	"Fibonacci.c"
	.option nopic
	.text
	.align	2
	.globl	main
	.type	main, @function
main:
	addi	sp,sp,-112
	sw	s0,108(sp)
	addi	s0,sp,112
	sw	zero,-100(s0)
	li	a5,1
	sw	a5,-96(s0)
	li	a5,2
	sw	a5,-20(s0)
	j	.L2
.L3:
	lw	a5,-20(s0)
	addi	a5,a5,-2
	slli	a5,a5,2
	addi	a4,s0,-16
	add	a5,a4,a5
	lw	a4,-84(a5)
	lw	a5,-20(s0)
	addi	a5,a5,-1
	slli	a5,a5,2
	addi	a3,s0,-16
	add	a5,a3,a5
	lw	a5,-84(a5)
	add	a4,a4,a5
	lw	a5,-20(s0)
	slli	a5,a5,2
	addi	a3,s0,-16
	add	a5,a3,a5
	sw	a4,-84(a5)
	lw	a5,-20(s0)
	addi	a5,a5,1
	sw	a5,-20(s0)
.L2:
	lw	a4,-20(s0)
	li	a5,19
	ble	a4,a5,.L3
	nop
	lw	s0,108(sp)
	addi	sp,sp,112
	jr	ra
	.size	main, .-main
	.ident	"GCC: (GNU) 7.2.0"
