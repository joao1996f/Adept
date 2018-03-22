	.file	"Fibonacci2.c"
	.option nopic
	.text
	.align	2
	.globl	main
	.type	main, @function
main:
	addi	sp,sp,-32
	sw	s0,28(sp)
	sw	s1,24(sp)
	sw	s2,20(sp)
	sw	s3,16(sp)
	sw	s4,12(sp)
	addi	s0,sp,32
	li	s3,0
	li	s2,1
	li	s1,0
	j	.L2
.L3:
	add	s4,s3,s2
	mv	s3,s2
	mv	s2,s4
	addi	s1,s1,1
.L2:
	li	a5,17
	ble	s1,a5,.L3
	nop
	lw	s0,28(sp)
	lw	s1,24(sp)
	lw	s2,20(sp)
	lw	s3,16(sp)
	lw	s4,12(sp)
	addi	sp,sp,32
	jr	ra
	.size	main, .-main
	.ident	"GCC: (GNU) 7.2.0"
