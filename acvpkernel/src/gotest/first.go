package main

import (
    "fmt"
    "math"

    "gonum.org/v1/gonum/stat"
)

func main() {
    xs := []float64{
        12.44, 11.2, 34.5, 1.4, 6.7, 23.4,
    }

    fmt.Printf("data: %v\n", xs)

    mean := stat.Mean(xs, nil)
    variance := stat.Variance(xs, nil)
    stddev := math.Sqrt(variance)

    fmt.Printf("mean: %v\n", mean)
    fmt.Printf("variance: %v\n", variance)
    fmt.Printf("std-dev: %v\n", stddev)
}