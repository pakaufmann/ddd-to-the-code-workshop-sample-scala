package com.github.pkaufmann.dddttc

package object testing {
  def always[A, B](out: => B): A => B = _ => out

  def always[A1, A2, B](out: => B): (A1, A2) => B = (_, _) => out
}
