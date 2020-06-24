	(import "host" "exit" (func $exit))
	(import "host" "getchar" (func $getchar (result i32)))
	(import "host" "putchar" (func $putchar (param i32)))
	(func $Dprintc (param $ascii i32) 
		local.get $ascii
		call $putchar
	)
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
				call $putchar
				
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
	)