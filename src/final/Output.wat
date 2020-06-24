(module
	(import "host" "exit" (func $Dhalt))
	(import "host" "getchar" (func $Dgetchar (result i32)))
	(import "host" "putchar" (func $Dprintc (param i32)))
	(func $Dprints (param $start i32) (param $length i32)
		(local $end i32)
		local.get $start
		local.get $length
		i32.add
		local.set $end
		(block $b0
			(loop $l0
				local.get $start
				local.get $end
				i32.ge_s
				(br_if $b0)
				
				local.get $start
				i32.load
				call $Dprintc
				
				local.get $start
				i32.const 1
				i32.add
				local.set $start
				(br $l0)
			)
		)
	)
	(func $Dprinti (param $x i32) 
		(local $rev i32)
		(local $oldX i32)
		
		local.get $x
		i32.eqz
		(if
			(then
				
				i32.const 48
				call $Dprintc
			)
			(else
				
				local.get $x
				i32.const 0
				i32.lt_s
				(if
					(then
						
						i32.const 45
						;; 45
						
						call $Dprintc
						
						i32.const 0
						local.get $x
						i32.sub
						local.set $x
					)
				)
			)
		)
		
		local.get $x
		local.set $oldX

		(loop $L0

			local.get $rev
			i32.const 10
			i32.mul
			local.set $rev
			
			local.get $rev
			local.get $x
			i32.const 10
			i32.rem_s
			i32.add
			local.set $rev
			
			local.get $x
			i32.const 10
			i32.div_s
			local.set $x
			
			local.get $x
			i32.const 0
			i32.gt_s
			
			br_if $L0
		)
		
		(loop $L1
			
			local.get $rev
			i32.const 10
			i32.rem_s
			i32.const 48
			i32.add			
			call $Dprintc
			
			local.get $rev
			i32.const 10
			i32.div_s
			local.set $rev
			
			local.get $oldX
			i32.const 10
			i32.div_s
			local.set $oldX
			
			local.get $oldX
			i32.const 0
			i32.gt_s
			
			br_if $L1
		)
	)	(func $main
		(local $boolTemp i32)
		i32.const 9
		i32.const 32
		call $Dprints
		i32.const 41
		i32.const 31
		call $Dprints
	)
	(start $main)
	(data 0 (i32.const 0) "true")
	(data 0 (i32.const 4) "false")
	(data 0 (i32.const 9) "\n\tthis is tabbed\nand this is not")
	(data 0 (i32.const 41) "making sure offsets are correct")
	(memory 1)
)

